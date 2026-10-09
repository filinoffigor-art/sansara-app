/**
 * SANSARA Apps Script API 0.2-alpha.
 * Store SHEET_ID and API_KEY in Script Properties, never in GitHub.
 */
function props_() { return PropertiesService.getScriptProperties(); }
function sheet_() { return SpreadsheetApp.openById(props_().getProperty('SHEET_ID')); }
function json_(data) { return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON); }
function auth_(body) { return body && body.apiKey && body.apiKey === props_().getProperty('API_KEY'); }

function doGet() { return json_({ok:true, service:'SANSARA API', version:'0.2-alpha'}); }

function eventAction_(event) {
  if (event === 'registration') return 'registerClient';
  if (event === 'order') return 'createOrder';
  if (event === 'presence') return 'presence';
  return event || '';
}

function payload_(body) {
  return body && body.payload ? body.payload : body;
}

function doPost(e) {
  var body = JSON.parse((e.postData && e.postData.contents) || '{}');
  if (!auth_(body)) return json_({ok:false, error:'UNAUTHORIZED'});
  var action = body.action || eventAction_(body.event);
  var payload = payload_(body);
  try {
    switch (action) {
      case 'catalog': return json_({ok:true, items:getCatalog_()});
      case 'registerClient': return json_(registerClient_(payload));
      case 'updateClient': return json_(updateClient_(payload));
      case 'postProduction': return json_(postProduction_(payload));
      case 'createOrder': return json_(createOrder_(payload));
      case 'orders': return json_({ok:true, orders:rows_('Orders')});
      case 'presence': return json_(presence_(payload));
      case 'telegramSettings': return json_(updateTelegramSettings_(payload));
      case 'telegramStatus': return json_(telegramStatus_());
      case 'syncTilda': return json_(syncTildaCatalog());
      default: return json_({ok:false, error:'UNKNOWN_ACTION'});
    }
  } catch (err) {
    return json_({ok:false, error:String(err && err.message || err)});
  }
}

function rows_(name) {
  var sh = sheet_().getSheetByName(name);
  if (!sh || sh.getLastRow() < 2) return [];
  var values = sh.getDataRange().getValues();
  var headers = values.shift();
  return values.map(function(row){ var o={}; headers.forEach(function(h,i){o[h]=row[i];}); return o; });
}

function appendObject_(name, obj) {
  var sh = sheet_().getSheetByName(name);
  var headers = sh.getRange(1,1,1,sh.getLastColumn()).getValues()[0];
  sh.appendRow(headers.map(function(h){ return obj[h] === undefined ? '' : obj[h]; }));
}

function getCatalog_() {
  var products = rows_('Products');
  var moves = rows_('StockMovements');
  var stock = {};
  moves.forEach(function(m){
    var sku = String(m.SKU || '');
    if (!stock[sku]) stock[sku] = {physical:0,reserved:0};
    var q = Number(m.Qty || 0);
    if (m.MovementType === 'PRODUCTION_IN' || m.MovementType === 'ADJUSTMENT_IN') stock[sku].physical += q;
    if (m.MovementType === 'SHIPMENT_OUT') stock[sku].physical -= q;
    if (m.MovementType === 'ORDER_RESERVE') stock[sku].reserved += q;
    if (m.MovementType === 'ORDER_RELEASE') stock[sku].reserved -= q;
  });
  return products.filter(function(p){return String(p.Active).toLowerCase() !== 'false';}).map(function(p){
    var s = stock[p.SKU] || {physical:0,reserved:0};
    p.Physical = s.physical;
    p.Reserved = s.reserved;
    p.Available = Math.max(0, s.physical - s.reserved);
    return p;
  });
}

function registerClient_(b) {
  var id = 'C-' + Utilities.getUuid().slice(0,8).toUpperCase();
  appendObject_('Clients', {
    ClientID:id, Type:b.type, Name:b.name, INN:b.inn, Phone:b.phone, Email:b.email,
    City:b.city, Address:b.address, Status:'NEW', DiscountPct:0, OrderingEnabled:false,
    ManagerID:'', CreatedAt:new Date()
  });
  return {ok:true, clientId:id, status:'NEW'};
}

function updateClient_(b) {
  var sh = sheet_().getSheetByName('Clients');
  var data = sh.getDataRange().getValues();
  var headers = data[0];
  var idCol = headers.indexOf('ClientID');
  var rowIndex = -1;
  for (var i=1;i<data.length;i++) if (String(data[i][idCol]) === String(b.clientId)) { rowIndex=i+1; break; }
  if (rowIndex < 0) throw new Error('CLIENT_NOT_FOUND');
  ['Status','DiscountPct','OrderingEnabled','ManagerID'].forEach(function(field){
    if (b[field] !== undefined) sh.getRange(rowIndex, headers.indexOf(field)+1).setValue(b[field]);
  });
  appendObject_('AuditLog',{Timestamp:new Date(),UserID:b.userId,EntityType:'CLIENT',EntityID:b.clientId,Action:'UPDATE_CLIENT',OldValue:'',NewValue:JSON.stringify(b)});
  return {ok:true};
}

function presence_(b) {
  var sh = sheet_().getSheetByName('ClientUsers');
  if (!sh || sh.getLastRow() < 2) return {ok:true, updated:false};
  var data = sh.getDataRange().getValues();
  var headers = data[0];
  var userCol = headers.indexOf('UserID');
  var seenCol = headers.indexOf('LastSeen');
  if (seenCol < 0) {
    seenCol = headers.length;
    sh.getRange(1, seenCol + 1).setValue('LastSeen');
  }
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][userCol]) === String(b.userId || '')) {
      sh.getRange(i + 1, seenCol + 1).setValue(new Date(Number(b.lastSeen || Date.now())));
      return {ok:true, updated:true};
    }
  }
  return {ok:true, updated:false};
}

function postProduction_(b) {
  (b.lines || []).forEach(function(line){
    if (Number(line.qty) <= 0) return;
    appendObject_('StockMovements',{Timestamp:new Date(),MovementType:'PRODUCTION_IN',SKU:line.sku,Qty:Number(line.qty),UserID:b.userId,Comment:b.comment || 'Ежедневный выпуск'});
  });
  return {ok:true};
}

function telegramStatus_() {
  var p = props_();
  var chats = String(p.getProperty('TELEGRAM_CHAT_IDS') || '').split(',').map(function(v){return v.trim();}).filter(String);
  return {
    ok:true,
    enabled:String(p.getProperty('TELEGRAM_ENABLED') || 'false').toLowerCase() === 'true',
    configured:!!p.getProperty('TELEGRAM_BOT_TOKEN'),
    chatCount:chats.length,
    chatIds:chats,
    template:p.getProperty('TELEGRAM_TEMPLATE') || ''
  };
}

function updateTelegramSettings_(b) {
  var p = props_();
  if (b.enabled !== undefined) p.setProperty('TELEGRAM_ENABLED', String(!!b.enabled));
  if (b.chatIds !== undefined) {
    var chats = Array.isArray(b.chatIds) ? b.chatIds : String(b.chatIds || '').split(',');
    p.setProperty('TELEGRAM_CHAT_IDS', chats.map(function(v){return String(v).trim();}).filter(String).join(','));
  }
  if (b.template !== undefined) p.setProperty('TELEGRAM_TEMPLATE', String(b.template || ''));
  return telegramStatus_();
}

function renderTelegramOrder_(orderId,b) {
  var template = props_().getProperty('TELEGRAM_TEMPLATE') ||
    'Новый заказ №{ORDER_ID}\nКлиент: {CLIENT}\nСостав: {LINES}\nСумма: {TOTAL}\nКонтакты: {CONTACTS}\nПолучение: {DELIVERY}\nДата/время: {DATE_TIME}';
  var lines=(b.lines || []).map(function(line){
    return (line.sku || '')+' × '+Number(line.qty || 0);
  }).join(', ');
  var values={
    ORDER_ID:orderId,
    CLIENT:String(b.client || b.clientName || b.clientId || ''),
    LINES:lines || String(b.linesText || ''),
    TOTAL:String(b.total || ''),
    CONTACTS:[b.recipient,b.contactPhone].filter(String).join(' · '),
    DELIVERY:[b.deliveryMethod,b.deliveryAddress].filter(String).join(' · '),
    DATE_TIME:[b.deliveryDate,b.deliveryTime].filter(String).join(' ')
  };
  Object.keys(values).forEach(function(key){
    template=template.split('{'+key+'}').join(values[key]);
  });
  return template;
}

function appendTelegramLog_(entry) {
  var sh=sheet_().getSheetByName('TelegramLog');
  if(!sh)return;
  appendObject_('TelegramLog',{
    Timestamp:new Date(),OrderID:entry.orderId || '',ChatID:entry.chatId || '',
    Attempt:entry.attempt || 0,Status:entry.status || '',Error:entry.error || ''
  });
}

function sendTelegramOrder_(orderId,b) {
  var p=props_();
  if(String(p.getProperty('TELEGRAM_ENABLED') || 'false').toLowerCase() !== 'true') return {ok:true,skipped:true};
  var token=p.getProperty('TELEGRAM_BOT_TOKEN');
  if(!token)return {ok:false,error:'TELEGRAM_BOT_TOKEN_NOT_CONFIGURED'};
  var chats=String(p.getProperty('TELEGRAM_CHAT_IDS') || '').split(',').map(function(v){return v.trim();}).filter(String);
  if(!chats.length)return {ok:false,error:'TELEGRAM_CHAT_IDS_NOT_CONFIGURED'};
  var text=renderTelegramOrder_(orderId,b);
  var failed=[];
  chats.forEach(function(chatId){
    var success=false,lastError='';
    for(var attempt=1;attempt<=3 && !success;attempt++){
      try{
        var response=UrlFetchApp.fetch('https://api.telegram.org/bot'+token+'/sendMessage',{
          method:'post',contentType:'application/json',muteHttpExceptions:true,
          payload:JSON.stringify({chat_id:chatId,text:text})
        });
        var code=response.getResponseCode();
        success=code>=200 && code<300;
        lastError=success?'':'HTTP '+code+': '+response.getContentText().slice(0,500);
      }catch(err){lastError=String(err && err.message || err);}
      appendTelegramLog_({orderId:orderId,chatId:chatId,attempt:attempt,status:success?'SENT':'FAILED',error:lastError});
      if(!success && attempt<3)Utilities.sleep(1000*attempt);
    }
    if(!success)failed.push(chatId+': '+lastError);
  });
  return failed.length?{ok:false,error:failed.join(' | ')}:{ok:true};
}

function createOrder_(b) {
  var orderId = 'S-' + Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'yyyyMMddHHmmss');
  appendObject_('Orders',{OrderID:orderId,ClientID:b.clientId,Status:'NEW',DeliveryMethod:b.deliveryMethod,DeliveryAddress:b.deliveryAddress,Total:b.total,CreatedAt:new Date(),ManagerID:''});
  (b.lines || []).forEach(function(line){
    appendObject_('OrderItems',{OrderID:orderId,SKU:line.sku,Qty:Number(line.qty),UnitPrice:Number(line.unitPrice),DiscountPct:Number(line.discountPct||0),LineTotal:Number(line.lineTotal)});
    appendObject_('StockMovements',{Timestamp:new Date(),MovementType:'ORDER_RESERVE',SKU:line.sku,Qty:Number(line.qty),UserID:b.clientId,Comment:orderId});
  });
  var telegram=sendTelegramOrder_(orderId,b);
  return {ok:true, orderId:orderId, telegram:telegram};
}

/**
 * Tilda catalog sync (server side).
 * Source: public YML feed URL stored in Script Property TILDA_YML_URL.
 * Updates only descriptive fields, price and photo in the Products sheet.
 * Stock lives in StockMovements and is never touched here.
 * New items get no stock movements, so the apps show them as "Под заказ".
 */
function ensureColumns_(sh, wanted) {
  var lastCol = Math.max(sh.getLastColumn(), 1);
  var headers = sh.getRange(1, 1, 1, lastCol).getValues()[0].map(String);
  wanted.forEach(function(name){
    if (headers.indexOf(name) < 0) {
      headers.push(name);
      sh.getRange(1, headers.length).setValue(name).setFontWeight('bold');
    }
  });
  return headers.filter(String);
}

function parseTildaYml_(xmlText) {
  var doc = XmlService.parse(xmlText);
  var shop = doc.getRootElement().getChild('shop');
  if (!shop) throw new Error('YML_SHOP_NOT_FOUND');
  var categories = {};
  var catsEl = shop.getChild('categories');
  if (catsEl) catsEl.getChildren('category').forEach(function(c){
    categories[String(c.getAttribute('id').getValue())] = String(c.getText()).trim();
  });
  var offersEl = shop.getChild('offers');
  if (!offersEl) throw new Error('YML_OFFERS_NOT_FOUND');
  var offers = offersEl.getChildren('offer').map(function(o){
    function t(n){ var e = o.getChild(n); return e ? String(e.getText()).trim() : ''; }
    var params = {};
    o.getChildren('param').forEach(function(p){
      var a = p.getAttribute('name');
      if (a) params[String(a.getValue()).toLowerCase()] = String(p.getText()).trim();
    });
    var idAttr = o.getAttribute('id');
    var externalId = idAttr ? String(idAttr.getValue()) : '';
    var pics = o.getChildren('picture');
    function p(){ // первое значение параметра, чьё имя содержит одно из слов
      for (var k in params) for (var i = 0; i < arguments.length; i++) if (k.indexOf(arguments[i]) >= 0 && params[k]) return params[k];
      return '';
    }
    var days = Number(String(params['срок производства'] || params['срок производства, дней'] || '').replace(/\D/g, '')) || 0;
    var catName = categories[t('categoryId')] || '';
    var group = p('продукция');
    return {
      externalId: externalId,
      sku: t('vendorCode') || externalId,
      name: t('name').split(' - ')[0],
      category: /круг|вен/i.test(group + ' ' + t('name')) ? 'Венки' : (group || catName),
      price: Math.round(Number(t('price').replace(',', '.')) || 0),
      image: pics.length ? String(pics[0].getText()).trim() : '',
      quality: p('качество', 'класс') || '',
      size: (p('размер', 'высота', 'диаметр') || '').replace(/(\d)\s*(см|м)\b/i, '$1 $2'),
      days: days,
      wholesale: /опт/i.test(catName) && !/рознич/i.test(catName),
      retail: /рознич/i.test(catName)
    };
  }).filter(function(x){ return x.sku && x.name; });
  return aggregateTilda_(offers);
}

/** Один артикул встречается в «Опт», «Розница» и по цветам. Цена приложения — оптовая, фото — с первого оптового предложения. */
function aggregateTilda_(offers) {
  var bySku = {}, order = [];
  offers.forEach(function(o){
    if (!bySku[o.sku]) { bySku[o.sku] = []; order.push(o.sku); }
    bySku[o.sku].push(o);
  });
  return order.map(function(sku){
    var list = bySku[sku];
    var w = list.filter(function(x){ return x.wholesale; });
    var r = list.filter(function(x){ return x.retail; });
    var primary = w[0] || r[0] || list[0];
    var photo = (w.concat(r, list)).filter(function(x){ return x.image; })[0];
    var item = JSON.parse(JSON.stringify(primary));
    item.image = photo ? photo.image : '';
    item.retailPrice = r.length ? r[0].price : 0;
    return item;
  });
}

function syncTildaCatalog() {
  var url = props_().getProperty('TILDA_YML_URL');
  if (!url) return {ok:false, error:'TILDA_YML_URL_NOT_CONFIGURED'};
  var resp = UrlFetchApp.fetch(url, {muteHttpExceptions:true, followRedirects:true});
  if (resp.getResponseCode() !== 200) return {ok:false, error:'TILDA_HTTP_' + resp.getResponseCode()};
  var items = parseTildaYml_(resp.getContentText('UTF-8'));
  if (!items.length) return {ok:false, error:'TILDA_FEED_EMPTY'};

  var sh = sheet_().getSheetByName('Products');
  if (!sh) throw new Error('PRODUCTS_SHEET_MISSING');
  var headers = ensureColumns_(sh, ['SKU','Name','CategoryID','Size','BasePrice','ProductionLeadDays','Active','ImageUrl','Quality','ExternalID','TildaSyncedAt','RetailPrice']);
  var idx = {};
  headers.forEach(function(h, i){ idx[h] = i; });

  var width = headers.length;
  var rows = sh.getLastRow() > 1 ? sh.getRange(2, 1, sh.getLastRow() - 1, width).getValues() : [];
  var rowBySku = {};
  rows.forEach(function(r, i){ rowBySku[String(r[idx.SKU]).trim().toLowerCase()] = i; });

  var now = new Date();
  var added = 0, updated = 0, fresh = [];
  items.forEach(function(it){
    var key = it.sku.toLowerCase();
    var row;
    if (key in rowBySku) {
      row = rows[rowBySku[key]];
      updated++;
    } else {
      row = [];
      for (var c = 0; c < width; c++) row.push('');
      row[idx.SKU] = it.sku;
      row[idx.Active] = true;
      row[idx.ProductionLeadDays] = 3;
      fresh.push(row);
      added++;
    }
    row[idx.Name] = it.name;
    if (it.category) row[idx.CategoryID] = it.category;
    if (it.size) row[idx.Size] = it.size;
    if (it.price > 0) row[idx.BasePrice] = it.price;
    if (it.days) row[idx.ProductionLeadDays] = it.days;
    if (it.image) row[idx.ImageUrl] = it.image;
    if (it.quality) row[idx.Quality] = it.quality;
    if (it.retailPrice) row[idx.RetailPrice] = it.retailPrice;
    row[idx.ExternalID] = it.externalId;
    row[idx.TildaSyncedAt] = now;
  });

  if (rows.length) sh.getRange(2, 1, rows.length, width).setValues(rows);
  if (fresh.length) sh.getRange(sh.getLastRow() + 1, 1, fresh.length, width).setValues(fresh);
  appendObject_('AuditLog', {Timestamp:now, UserID:'SYSTEM', EntityType:'CATALOG', EntityID:'TILDA', Action:'SYNC_TILDA', OldValue:'', NewValue:JSON.stringify({total:items.length, added:added, updated:updated})});
  return {ok:true, total:items.length, added:added, updated:updated};
}

/** Run once: syncs the Tilda catalog automatically every hour. */
function installTildaTrigger() {
  ScriptApp.getProjectTriggers().forEach(function(t){
    if (t.getHandlerFunction() === 'syncTildaCatalog') ScriptApp.deleteTrigger(t);
  });
  ScriptApp.newTrigger('syncTildaCatalog').timeBased().everyHours(1).create();
}

function setupSansaraSheets() {
  var ss = sheet_();
  var schemas = {
    Products:['SKU','ModelID','Name','CategoryID','Size','BasePrice','ProductionLeadDays','Active','ImageUrl','Quality','ExternalID','TildaSyncedAt'],
    Clients:['ClientID','Type','Name','INN','Phone','Email','City','Address','Status','DiscountPct','OrderingEnabled','ManagerID','CreatedAt'],
    ClientUsers:['UserID','ClientID','Name','Phone','Email','Role','Enabled','LastLogin','LastSeen'],
    StockMovements:['Timestamp','MovementType','SKU','Qty','UserID','Comment'],
    Orders:['OrderID','ClientID','Status','DeliveryMethod','DeliveryAddress','Total','CreatedAt','ManagerID'],
    OrderItems:['OrderID','SKU','Qty','UnitPrice','DiscountPct','LineTotal'],
    AuditLog:['Timestamp','UserID','EntityType','EntityID','Action','OldValue','NewValue'],
    TelegramLog:['Timestamp','OrderID','ChatID','Attempt','Status','Error']
  };
  Object.keys(schemas).forEach(function(name){
    var sh = ss.getSheetByName(name) || ss.insertSheet(name);
    sh.clear();
    sh.getRange(1,1,1,schemas[name].length).setValues([schemas[name]]).setFontWeight('bold');
    sh.setFrozenRows(1);
  });
}
