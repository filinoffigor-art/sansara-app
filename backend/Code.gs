/**
 * SANSARA Apps Script API 0.2-alpha.
 * Store SHEET_ID and API_KEY in Script Properties, never in GitHub.
 */
function props_() { return PropertiesService.getScriptProperties(); }
function sheet_() { return SpreadsheetApp.openById(props_().getProperty('SHEET_ID')); }
function json_(data) { return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON); }
function auth_(body) { return body && body.apiKey && body.apiKey === props_().getProperty('API_KEY'); }

function doGet() { return json_({ok:true, service:'SANSARA API', version:'0.2-alpha'}); }

function doPost(e) {
  var body = JSON.parse((e.postData && e.postData.contents) || '{}');
  if (!auth_(body)) return json_({ok:false, error:'UNAUTHORIZED'});
  try {
    switch (body.action) {
      case 'catalog': return json_({ok:true, items:getCatalog_()});
      case 'registerClient': return json_(registerClient_(body));
      case 'updateClient': return json_(updateClient_(body));
      case 'postProduction': return json_(postProduction_(body));
      case 'createOrder': return json_(createOrder_(body));
      case 'orders': return json_({ok:true, orders:rows_('Orders')});
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

function postProduction_(b) {
  (b.lines || []).forEach(function(line){
    if (Number(line.qty) <= 0) return;
    appendObject_('StockMovements',{Timestamp:new Date(),MovementType:'PRODUCTION_IN',SKU:line.sku,Qty:Number(line.qty),UserID:b.userId,Comment:b.comment || 'Ежедневный выпуск'});
  });
  return {ok:true};
}

function createOrder_(b) {
  var orderId = 'S-' + Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'yyyyMMddHHmmss');
  appendObject_('Orders',{OrderID:orderId,ClientID:b.clientId,Status:'NEW',DeliveryMethod:b.deliveryMethod,DeliveryAddress:b.deliveryAddress,Total:b.total,CreatedAt:new Date(),ManagerID:''});
  (b.lines || []).forEach(function(line){
    appendObject_('OrderItems',{OrderID:orderId,SKU:line.sku,Qty:Number(line.qty),UnitPrice:Number(line.unitPrice),DiscountPct:Number(line.discountPct||0),LineTotal:Number(line.lineTotal)});
    appendObject_('StockMovements',{Timestamp:new Date(),MovementType:'ORDER_RESERVE',SKU:line.sku,Qty:Number(line.qty),UserID:b.clientId,Comment:orderId});
  });
  return {ok:true, orderId:orderId};
}

function setupSansaraSheets() {
  var ss = sheet_();
  var schemas = {
    Products:['SKU','ModelID','Name','CategoryID','Size','BasePrice','ProductionLeadDays','Active','ImageUrl'],
    Clients:['ClientID','Type','Name','INN','Phone','Email','City','Address','Status','DiscountPct','OrderingEnabled','ManagerID','CreatedAt'],
    ClientUsers:['UserID','ClientID','Name','Phone','Email','Role','Enabled','LastLogin'],
    StockMovements:['Timestamp','MovementType','SKU','Qty','UserID','Comment'],
    Orders:['OrderID','ClientID','Status','DeliveryMethod','DeliveryAddress','Total','CreatedAt','ManagerID'],
    OrderItems:['OrderID','SKU','Qty','UnitPrice','DiscountPct','LineTotal'],
    AuditLog:['Timestamp','UserID','EntityType','EntityID','Action','OldValue','NewValue']
  };
  Object.keys(schemas).forEach(function(name){
    var sh = ss.getSheetByName(name) || ss.insertSheet(name);
    sh.clear();
    sh.getRange(1,1,1,schemas[name].length).setValues([schemas[name]]).setFontWeight('bold');
    sh.setFrozenRows(1);
  });
}
