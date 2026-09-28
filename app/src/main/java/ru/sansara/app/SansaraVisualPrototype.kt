package ru.sansara.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private val ProtoBg = Color(0xFF0A0A09)
private val ProtoPanel = Color(0xFF171613)
private val ProtoPanel2 = Color(0xFF211E19)
private val ProtoGold = Color(0xFFD8B07C)
private val ProtoGoldSoft = Color(0xFFE7C59C)
private val ProtoText = Color(0xFFF5F0E8)
private val ProtoMuted = Color(0xFF9D9992)
private val ProtoGreen = Color(0xFF4BE087)
private val ProtoRed = Color(0xFFFF746C)
private val ProtoBorder = Color(0xFF4B4033)

private enum class ProtoScreen(val drawable: Int? = null) {
    Welcome(R.drawable.screen_welcome),
    Registration(R.drawable.screen_registration),
    RegistrationSent(R.drawable.screen_reg_sent),
    Home(R.drawable.screen_client_home),
    Catalog(R.drawable.screen_catalog),
    Filter,
    ProductList,
    ProductDetail,
    Cart,
    Checkout(R.drawable.screen_checkout),
    OrderSent(R.drawable.screen_order_sent),
    Suspended(R.drawable.screen_suspended),
    AdminHome(R.drawable.screen_admin_home),
    ClientCard(R.drawable.screen_client_card),
    Production(R.drawable.screen_production),
    ProductionCatalog,
    ProductionEntry,
    ProductionHistory,
    Server(R.drawable.screen_server)
}

private data class ProtoCatalogProduct(
    val sku: String,
    val name: String,
    val type: String,
    val quality: String,
    val size: String,
    val price: Int,
    val stock: Int,
    val status: String,
    val productionDays: Int
)

private data class ProtoProductionOp(
    val date: String,
    val time: String,
    val sku: String,
    val name: String,
    val qty: Int,
    val assembler: String,
    val postedBy: String,
    val status: String = "Проведен"
)

@Composable
fun SansaraVisualPrototype() {
    val context = LocalContext.current
    val products = remember { protoLoadProducts(context) }
    var screen by remember { mutableStateOf(ProtoScreen.Welcome) }
    val history: SnapshotStateList<ProtoScreen> = remember { mutableStateListOf() }
    var showRolePicker by remember { mutableStateOf(false) }

    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedQualities by remember { mutableStateOf(setOf<String>()) }
    var selectedWreathSizes by remember { mutableStateOf(setOf<String>()) }
    var selectedBasketSizes by remember { mutableStateOf(setOf<String>()) }
    var selectedAvailability by remember { mutableStateOf(setOf<String>()) }
    var selectedProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    val cart: SnapshotStateMap<String, Int> = remember { mutableStateMapOf() }
    val stockOverrides: SnapshotStateMap<String, Int> = remember { mutableStateMapOf() }
    var productionProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var productionQty by remember { mutableIntStateOf(1) }
    var productionAssembler by remember { mutableStateOf("Анна К.") }
    val productionOps: SnapshotStateList<ProtoProductionOp> = remember {
        mutableStateListOf(
            ProtoProductionOp("28.09.2026","08:12","V-060-001","Венок Премиум 60 см №01",6,"Анна К.","Игорь Ф."),
            ProtoProductionOp("28.09.2026","08:24","V-060-007","Венок Стандарт 60 см №07",8,"Мария С.","Игорь Ф."),
            ProtoProductionOp("28.09.2026","08:41","V-140-005","Венок Эконом 140 см №05",5,"Елена П.","Игорь Ф."),
            ProtoProductionOp("27.09.2026","14:32","V-060-010","Венок Стандарт 60 см №10",10,"Анна К.","Петров И.А."),
            ProtoProductionOp("27.09.2026","13:18","V-125-009","Венок Эконом 125 см №09",7,"Мария С.","Петров И.А."),
            ProtoProductionOp("26.09.2026","16:47","V-060-001","Венок Премиум 60 см №01",9,"Анна К.","Смирнова Е.В.")
        )
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun go(target: ProtoScreen) {
        if (target != screen) {
            history.add(screen)
            screen = target
        }
    }
    fun back() {
        screen = if (history.isNotEmpty()) history.removeAt(history.lastIndex) else ProtoScreen.Welcome
    }
    fun openWreathFilter() {
        selectedTypes = setOf("Венки")
        selectedQualities = emptySet()
        selectedWreathSizes = emptySet()
        selectedBasketSizes = emptySet()
        selectedAvailability = emptySet()
        go(ProtoScreen.Filter)
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = ProtoGold, background = ProtoBg, surface = ProtoPanel)) {
        Box(Modifier.fillMaxSize().background(ProtoBg)) {
            when (screen) {
                ProtoScreen.Filter -> ProtoFilterScreen(
                    selectedTypes, selectedQualities, selectedWreathSizes, selectedBasketSizes, selectedAvailability,
                    onToggleType = { selectedTypes = protoToggle(selectedTypes, it) },
                    onToggleQuality = { selectedQualities = protoToggle(selectedQualities, it) },
                    onToggleWreathSize = { selectedWreathSizes = protoToggle(selectedWreathSizes, it) },
                    onToggleBasketSize = { selectedBasketSizes = protoToggle(selectedBasketSizes, it) },
                    onToggleAvailability = { selectedAvailability = protoToggle(selectedAvailability, it) },
                    onShow = { go(ProtoScreen.ProductList) },
                    onBack = { back() }
                )
                ProtoScreen.ProductionCatalog -> ProtoProductionCatalogScreen(
                    products = products.filter { it.type == "Венки" || it.type == "Венки круглые" },
                    onBack = { back() },
                    onSelect = { productionProduct = it; productionQty = 1; go(ProtoScreen.ProductionEntry) }
                )
                ProtoScreen.ProductionEntry -> ProtoProductionEntryScreen(
                    product = productionProduct,
                    qty = productionQty,
                    assembler = productionAssembler,
                    onBack = { back() },
                    onMinus = { productionQty = (productionQty - 1).coerceAtLeast(1) },
                    onPlus = { productionQty += 1 },
                    onAssembler = { productionAssembler = it },
                    onPost = {
                        productionProduct?.let { p ->
                            val current = stockOverrides[p.sku] ?: p.stock
                            stockOverrides[p.sku] = current + productionQty
                            productionOps.add(0, ProtoProductionOp("28.09.2026","09:20",p.sku,p.name,productionQty,productionAssembler,"Игорь Ф."))
                            toast("Оприходовано: ${p.name} +${productionQty} шт.")
                            go(ProtoScreen.ProductionHistory)
                        }
                    }
                )
                ProtoScreen.ProductionHistory -> ProtoProductionHistoryScreen(
                    ops = productionOps,
                    onBack = { back() }
                )
                ProtoScreen.ProductList -> {
                    val filtered = products.filter { p ->
                        (selectedTypes.isEmpty() || p.type in selectedTypes) &&
                        (selectedQualities.isEmpty() || p.quality == "—" || p.quality in selectedQualities) &&
                        (selectedWreathSizes.isEmpty() || p.type !in setOf("Венки","Венки круглые") || p.size in selectedWreathSizes) &&
                        (selectedBasketSizes.isEmpty() || p.type != "Корзины" || p.size in selectedBasketSizes) &&
                        (selectedAvailability.isEmpty() || (if ((stockOverrides[p.sku] ?: p.stock) > 0) "В наличии" else "Под заказ") in selectedAvailability)
                    }
                    ProtoProductListScreen(
                        products = filtered,
                        cart = cart,
                        stockOverrides = stockOverrides,
                        onBack = { back() },
                        onOpenFilter = { go(ProtoScreen.Filter) },
                        onOpenProduct = { selectedProduct = it; go(ProtoScreen.ProductDetail) },
                        onAdd = { p -> cart[p.sku] = (cart[p.sku] ?: 0) + 1; toast("${p.name} добавлен в корзину") },
                        onCart = { go(ProtoScreen.Cart) }
                    )
                }
                ProtoScreen.ProductDetail -> ProtoProductDetailScreen(
                    product = selectedProduct,
                    currentStock = selectedProduct?.let { stockOverrides[it.sku] ?: it.stock } ?: 0,
                    qty = selectedProduct?.let { cart[it.sku] ?: 1 } ?: 1,
                    onBack = { back() },
                    onMinus = { selectedProduct?.let { p -> cart[p.sku] = ((cart[p.sku] ?: 1) - 1).coerceAtLeast(1) } },
                    onPlus = { selectedProduct?.let { p -> cart[p.sku] = (cart[p.sku] ?: 1) + 1 } },
                    onAdd = { selectedProduct?.let { p -> if ((cart[p.sku] ?: 0) == 0) cart[p.sku] = 1; go(ProtoScreen.Cart) } }
                )
                ProtoScreen.Cart -> ProtoCartScreen(
                    products = products,
                    cart = cart,
                    onBack = { back() },
                    onPlus = { p -> cart[p.sku] = (cart[p.sku] ?: 0) + 1 },
                    onMinus = { p -> val n=(cart[p.sku] ?: 1)-1; if(n<=0) cart.remove(p.sku) else cart[p.sku]=n },
                    onDelete = { p -> cart.remove(p.sku) },
                    onCheckout = { go(ProtoScreen.Checkout) }
                )
                else -> {
                    val drawable = screen.drawable
                    if (drawable != null) {
                        Image(
                            painter = painterResource(drawable),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            @Composable
                            fun hotspot(x: Float, y: Float, w: Float, h: Float, action: () -> Unit) {
                                PrototypeClickArea(maxWidth, maxHeight, x, y, w, h, action)
                            }
                            hotspot(0.20f, 0.045f, 0.60f, 0.10f) { showRolePicker = true }
                            when (screen) {
                                ProtoScreen.Welcome -> {
                                    hotspot(.08f,.70f,.84f,.075f){ go(ProtoScreen.Home) }
                                    hotspot(.08f,.78f,.84f,.075f){ go(ProtoScreen.Registration) }
                                    hotspot(.20f,.91f,.60f,.06f){ protoDial(context) }
                                }
                                ProtoScreen.Registration -> {
                                    hotspot(.16f,.67f,.34f,.055f){ toast("Тип клиента: Агент") }
                                    hotspot(.50f,.67f,.40f,.055f){ toast("Тип клиента: Торгующая организация") }
                                    hotspot(.08f,.82f,.84f,.075f){ go(ProtoScreen.RegistrationSent) }
                                    hotspot(.35f,.91f,.35f,.055f){ go(ProtoScreen.Home) }
                                }
                                ProtoScreen.RegistrationSent -> {
                                    hotspot(.08f,.78f,.84f,.075f){ go(ProtoScreen.Home) }
                                    hotspot(.20f,.87f,.64f,.055f){ protoDial(context) }
                                }
                                ProtoScreen.Home -> {
                                    hotspot(.08f,.20f,.84f,.06f){ toast("Поиск по артикулу и названию") }
                                    hotspot(.08f,.27f,.28f,.05f){ selectedAvailability=setOf("В наличии"); go(ProtoScreen.ProductList) }
                                    hotspot(.38f,.27f,.30f,.05f){ selectedAvailability=setOf("Под заказ"); go(ProtoScreen.ProductList) }
                                    hotspot(.06f,.33f,.15f,.11f){ openWreathFilter() }
                                    hotspot(.22f,.33f,.15f,.11f){ toast("Категория «Гробы» будет подключена к Tilda") }
                                    hotspot(.38f,.33f,.15f,.11f){ toast("Категория «Одежда» будет подключена к Tilda") }
                                    hotspot(.54f,.33f,.15f,.11f){ toast("Категория «Ленты» будет подключена к Tilda") }
                                    hotspot(.70f,.33f,.13f,.11f){ toast("Категория «Цветы» будет подключена к Tilda") }
                                    hotspot(.84f,.33f,.12f,.11f){ toast("Категория «Услуги» будет подключена к Tilda") }
                                    hotspot(.06f,.49f,.43f,.36f){ selectedProduct=products.firstOrNull(); go(ProtoScreen.ProductDetail) }
                                    hotspot(.51f,.49f,.43f,.36f){ selectedProduct=products.getOrNull(1); go(ProtoScreen.ProductDetail) }
                                    protoClientBottomNav(maxWidth,maxHeight,{go(it)}, cart.values.sum())
                                }
                                ProtoScreen.Catalog -> {
                                    hotspot(.05f,.31f,.46f,.22f){ openWreathFilter() }
                                    hotspot(.05f,.24f,.22f,.05f){ selectedAvailability=emptySet(); go(ProtoScreen.ProductList) }
                                    hotspot(.29f,.24f,.28f,.05f){ selectedAvailability=setOf("В наличии"); go(ProtoScreen.ProductList) }
                                    hotspot(.59f,.24f,.30f,.05f){ selectedAvailability=setOf("Под заказ"); go(ProtoScreen.ProductList) }
                                    protoClientBottomNav(maxWidth,maxHeight,{go(it)}, cart.values.sum())
                                }
                                ProtoScreen.Checkout -> {
                                    hotspot(.08f,.32f,.28f,.06f){ toast("Способ получения: Доставка") }
                                    hotspot(.37f,.32f,.28f,.06f){ toast("Способ получения: Самовывоз") }
                                    hotspot(.66f,.32f,.26f,.06f){ toast("Способ получения: ТК") }
                                    hotspot(.08f,.79f,.84f,.075f){ cart.clear(); go(ProtoScreen.OrderSent) }
                                    protoClientBottomNav(maxWidth,maxHeight,{go(it)}, cart.values.sum())
                                }
                                ProtoScreen.OrderSent -> {
                                    hotspot(.08f,.81f,.84f,.07f){ go(ProtoScreen.Catalog) }
                                    protoClientBottomNav(maxWidth,maxHeight,{go(it)}, cart.values.sum())
                                }
                                ProtoScreen.Suspended -> {
                                    hotspot(.08f,.70f,.84f,.075f){ protoDial(context) }
                                    hotspot(.08f,.79f,.84f,.07f){ protoMessage(context) }
                                    protoClientBottomNav(maxWidth,maxHeight,{go(it)}, cart.values.sum())
                                }
                                ProtoScreen.AdminHome -> {
                                    hotspot(.07f,.23f,.43f,.09f){ go(ProtoScreen.ClientCard) }
                                    hotspot(.51f,.32f,.42f,.09f){ go(ProtoScreen.Production) }
                                    hotspot(.07f,.41f,.86f,.07f){ go(ProtoScreen.Server) }
                                    hotspot(.63f,.49f,.18f,.10f){ go(ProtoScreen.Catalog) }
                                    protoAdminBottomNav(maxWidth,maxHeight,{go(it)})
                                }
                                ProtoScreen.ClientCard -> {
                                    hotspot(.74f,.35f,.23f,.05f){ go(ProtoScreen.Suspended) }
                                    hotspot(.68f,.52f,.27f,.06f){ protoDial(context) }
                                    protoAdminBottomNav(maxWidth,maxHeight,{go(it)})
                                }
                                ProtoScreen.Production -> {
                                    hotspot(.07f,.22f,.86f,.06f){ go(ProtoScreen.ProductionCatalog) }
                                    hotspot(.07f,.48f,.86f,.06f){ go(ProtoScreen.ProductionCatalog) }
                                    hotspot(.10f,.63f,.80f,.07f){ go(ProtoScreen.ProductionCatalog) }
                                    hotspot(.10f,.71f,.80f,.06f){ go(ProtoScreen.ProductionHistory) }
                                    protoProductionBottomNav(maxWidth,maxHeight,{go(it)})
                                }
                                ProtoScreen.Server -> protoAdminBottomNav(maxWidth,maxHeight,{go(it)})
                                else -> Unit
                            }
                        }
                    }
                    if (screen != ProtoScreen.Welcome) {
                        ProtoBackButton(onClick = { back() })
                    }
                }
            }
        }

        if (showRolePicker) {
            AlertDialog(
                onDismissRequest = { showRolePicker = false },
                containerColor = ProtoPanel,
                title = { Text("Тестовый режим SANSARA", color=ProtoText) },
                text = { Text("Выберите роль для проверки интерфейса", color=ProtoMuted) },
                confirmButton = {
                    Column {
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.Welcome; showRolePicker=false }) { Text("Клиент", color=ProtoGold) }
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.AdminHome; showRolePicker=false }) { Text("Администратор", color=ProtoGold) }
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.Production; showRolePicker=false }) { Text("Производство", color=ProtoGold) }
                    }
                }
            )
        }
    }
}

@Composable
private fun ProtoBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 14.dp, top = 34.dp)
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xCC11100E))
            .border(1.dp, ProtoGold, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.ArrowBack, null, tint=ProtoGold, modifier=Modifier.size(25.dp))
    }
}

@Composable
private fun ProtoFilterScreen(
    selectedTypes:Set<String>, selectedQualities:Set<String>, selectedWreathSizes:Set<String>, selectedBasketSizes:Set<String>, selectedAvailability:Set<String>,
    onToggleType:(String)->Unit, onToggleQuality:(String)->Unit, onToggleWreathSize:(String)->Unit, onToggleBasketSize:(String)->Unit, onToggleAvailability:(String)->Unit,
    onShow:()->Unit, onBack:()->Unit
) {
    Column(Modifier.fillMaxSize().background(ProtoBg).padding(horizontal=20.dp)) {
        Spacer(Modifier.height(34.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            ProtoCircleBack(onBack)
            Spacer(Modifier.width(14.dp))
            Column { Text("Фильтры каталога", color=ProtoText, fontSize=29.sp, fontWeight=FontWeight.Bold); Text("Выберите параметры продукции", color=ProtoMuted, fontSize=14.sp) }
        }
        Spacer(Modifier.height(18.dp))
        ProtoFilterGroup("Продукция", listOf("Венки","Венки круглые","Корзины","Полянки","Флоретки"), selectedTypes, onToggleType)
        ProtoFilterGroup("Качество венков", listOf("Премиум","Стандарт","Эконом"), selectedQualities, onToggleQuality)
        ProtoFilterGroup("Размер венков", listOf("60 см","90 см","110 см","125 см","140 см"), selectedWreathSizes, onToggleWreathSize)
        ProtoFilterGroup("Размер корзин", listOf("30 см","70 см","100 см"), selectedBasketSizes, onToggleBasketSize)
        ProtoFilterGroup("Наличие", listOf("В наличии","Под заказ"), selectedAvailability, onToggleAvailability)
        Spacer(Modifier.weight(1f))
        Button(onClick=onShow, modifier=Modifier.fillMaxWidth().height(58.dp), colors=ButtonDefaults.buttonColors(containerColor=ProtoGold), shape=RoundedCornerShape(15.dp)) {
            Icon(Icons.Outlined.Tune,null,tint=Color.Black); Spacer(Modifier.width(10.dp)); Text("Показать товары", color=Color.Black, fontWeight=FontWeight.Bold, fontSize=18.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProtoFilterGroup(title:String, options:List<String>, selected:Set<String>, toggle:(String)->Unit) {
    Text(title, color=ProtoGoldSoft, fontSize=15.sp, fontWeight=FontWeight.SemiBold, modifier=Modifier.padding(top=9.dp,bottom=5.dp))
    Column(Modifier.fillMaxWidth().background(ProtoPanel, RoundedCornerShape(12.dp)).padding(4.dp)) {
        options.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable{toggle(option)}.padding(horizontal=8.dp,vertical=4.dp), verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=option in selected,onCheckedChange=null,colors=CheckboxDefaults.colors(checkedColor=ProtoGold,checkmarkColor=Color.Black,uncheckedColor=ProtoMuted))
                Text(option,color=ProtoText,fontSize=15.sp)
            }
        }
    }
}

@Composable
private fun ProtoProductListScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit,onOpenFilter:()->Unit,onOpenProduct:(ProtoCatalogProduct)->Unit,onAdd:(ProtoCatalogProduct)->Unit,onCart:()->Unit) {
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        Row(Modifier.padding(start=16.dp,end=16.dp,top=30.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically) {
            ProtoCircleBack(onBack); Spacer(Modifier.width(12.dp));
            Column(Modifier.weight(1f)){Text("Каталог",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Найдено: ${products.size} позиций",color=ProtoMuted)}
            BadgedBox(badge={if(cart.values.sum()>0) Badge(containerColor=ProtoGold){Text(cart.values.sum().toString(),color=Color.Black)}}) {
                IconButton(onClick=onCart){Icon(Icons.Outlined.ShoppingCart,null,tint=ProtoGold)}
            }
        }
        OutlinedButton(onClick=onOpenFilter,modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth(),border=BorderStroke(1.dp,ProtoBorder),colors=ButtonDefaults.outlinedButtonColors(contentColor=ProtoGold)) {Icon(Icons.Outlined.Tune,null);Spacer(Modifier.width(8.dp));Text("Изменить фильтр")}
        if(products.isEmpty()) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("По выбранным фильтрам товаров нет",color=ProtoMuted)}
        } else {
            LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(products,key={it.sku}) { p -> ProtoProductCard(p, stockOverrides[p.sku] ?: p.stock, onOpenProduct,onAdd) }
            }
        }
    }
}

@Composable
private fun ProtoProductCard(p:ProtoCatalogProduct,currentStock:Int,onOpen:(ProtoCatalogProduct)->Unit,onAdd:(ProtoCatalogProduct)->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(125.dp).background(ProtoPanel2).clickable{onOpen(p)},contentAlignment=Alignment.Center) {
            Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(56.dp))
            Surface(modifier=Modifier.align(Alignment.BottomStart).padding(8.dp),color=if(currentStock>0) Color(0xDD123A27) else Color(0xDD4A2220),shape=RoundedCornerShape(20.dp)) {
                Text(if(currentStock>0) "В наличии $currentStock" else "Под заказ · от ${p.productionDays} дней",color=if(currentStock>0) ProtoGreen else ProtoGoldSoft,fontSize=10.sp,modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp))
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=11.sp)
            Text(listOf(p.quality,p.size).filter{it!="—"}.joinToString(" · "),color=ProtoMuted,fontSize=11.sp)
            Spacer(Modifier.height(5.dp))
            Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=19.sp)
            Spacer(Modifier.height(7.dp))
            Button(onClick={onAdd(p)},modifier=Modifier.fillMaxWidth().height(40.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.AddShoppingCart,null,tint=Color.Black,modifier=Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text(if(currentStock>0)"В корзину" else "Под заказ",color=Color.Black,fontSize=12.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProtoProductDetailScreen(product:ProtoCatalogProduct?,currentStock:Int,qty:Int,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAdd:()->Unit) {
    val p=product ?: return
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        Box(Modifier.fillMaxWidth().height(390.dp).background(ProtoPanel2)) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(120.dp))}
            Box(Modifier.padding(start=14.dp,top=34.dp)){ProtoCircleBack(onBack)}
        }
        Column(Modifier.padding(18.dp)) {
            Text(p.name,color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold)
            Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=16.sp)
            Spacer(Modifier.height(10.dp))
            Row{Text(protoMoney(p.price),color=ProtoText,fontSize=32.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(if(currentStock>0)"● В наличии $currentStock шт." else "● Под заказ · от ${p.productionDays} дней",color=if(currentStock>0) ProtoGreen else ProtoGoldSoft,fontSize=13.sp)}
            Spacer(Modifier.height(15.dp))
            Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder)) {Column(Modifier.padding(15.dp)){ProtoInfoRow("Тип",p.type);ProtoInfoRow("Качество",p.quality);ProtoInfoRow("Размер",p.size)}}
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                OutlinedButton(onClick=onMinus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)}
                Text(qty.toString(),color=ProtoText,fontSize=22.sp,modifier=Modifier.padding(horizontal=18.dp))
                OutlinedButton(onClick=onPlus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}
                Spacer(Modifier.width(12.dp))
                Button(onClick=onAdd,modifier=Modifier.weight(1f).height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Icon(Icons.Outlined.ShoppingCart,null,tint=Color.Black);Spacer(Modifier.width(8.dp));Text("В корзину",color=Color.Black,fontWeight=FontWeight.Bold)}
            }
        }
    }
}

@Composable
private fun ProtoCartScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,onBack:()->Unit,onPlus:(ProtoCatalogProduct)->Unit,onMinus:(ProtoCatalogProduct)->Unit,onDelete:(ProtoCatalogProduct)->Unit,onCheckout:()->Unit) {
    val lines=cart.mapNotNull { (sku,qty) -> products.firstOrNull{it.sku==sku}?.let{it to qty} }
    val total=lines.sumOf { it.first.price*it.second }
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        Row(Modifier.padding(start=16.dp,end=16.dp,top=30.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Column{Text("Корзина",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("${lines.sumOf{it.second}} товаров на сумму ${protoMoney(total)}",color=ProtoMuted)}}
        if(lines.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Корзина пока пуста",color=ProtoMuted)} else {
            Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
                lines.take(5).forEach { (p,qty) ->
                    Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(vertical=5.dp)) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(70.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(35.dp))}
                            Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=11.sp);Spacer(Modifier.height(6.dp));Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={onMinus(p)},modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText);IconButton(onClick={onPlus(p)},modifier=Modifier.size(32.dp)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}}
                            Column(horizontalAlignment=Alignment.End){Text(protoMoney(p.price*qty),color=ProtoText,fontWeight=FontWeight.Bold);IconButton(onClick={onDelete(p)}){Icon(Icons.Outlined.Delete,null,tint=ProtoGold)}}
                        }
                    }
                }
            }
            Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.padding(14.dp).fillMaxWidth()) {Column(Modifier.padding(16.dp)){Row{Text("Товары:",color=ProtoMuted);Spacer(Modifier.weight(1f));Text(protoMoney(total),color=ProtoText)};Spacer(Modifier.height(8.dp));Row{Text("Итого:",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(protoMoney(total),color=ProtoText,fontSize=24.sp,fontWeight=FontWeight.Bold)}}}
            Button(onClick=onCheckout,enabled=lines.isNotEmpty(),modifier=Modifier.padding(horizontal=14.dp).fillMaxWidth().height(58.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(14.dp)){Text("Оформить заказ",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=18.sp);Spacer(Modifier.weight(1f));Icon(Icons.Outlined.ArrowForward,null,tint=Color.Black)}
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProtoProductionCatalogScreen(products:List<ProtoCatalogProduct>, onBack:()->Unit, onSelect:(ProtoCatalogProduct)->Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = products.filter { query.isBlank() || it.sku.contains(query, true) || it.name.contains(query, true) }
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        Row(Modifier.padding(start=16.dp,end=16.dp,top=30.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically) {
            ProtoCircleBack(onBack); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text("Каталог производства",color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold); Text("Выберите модель — артикул подставится автоматически",color=ProtoMuted,fontSize=12.sp) }
        }
        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth(),placeholder={Text("Поиск по артикулу или названию")},leadingIcon={Icon(Icons.Outlined.Search,null)},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLeadingIconColor=ProtoGold,unfocusedLeadingIconColor=ProtoMuted),singleLine=true)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier=Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            filtered.forEach { p ->
                item(key=p.sku) {
                    Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{onSelect(p)}) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(62.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(34.dp))}
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2);Text("Арт. ${p.sku}",color=ProtoGoldSoft,fontSize=12.sp);Text("${p.quality} · ${p.size}",color=ProtoMuted,fontSize=12.sp)}
                            Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtoProductionEntryScreen(product:ProtoCatalogProduct?,qty:Int,assembler:String,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAssembler:(String)->Unit,onPost:()->Unit) {
    val p=product ?: return
    Column(Modifier.fillMaxSize().background(ProtoBg).padding(horizontal=18.dp)) {
        Spacer(Modifier.height(34.dp)); Row(verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Text("Приход продукции",color=ProtoText,fontSize=29.sp,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.height(18.dp))
        Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp)){Text(p.name,color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Арт. ${p.sku}",color=ProtoGoldSoft);Text("${p.quality} · ${p.size}",color=ProtoMuted)}
        }
        Spacer(Modifier.height(18.dp));Text("Сборщица",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("Анна К.","Мария С.","Елена П.").forEach { name ->
                FilterChip(selected=assembler==name,onClick={onAssembler(name)},label={Text(name)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))
            }
        }
        Spacer(Modifier.height(22.dp));Text("Количество выпущено",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){OutlinedButton(onClick=onMinus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText,fontSize=34.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=30.dp));OutlinedButton(onClick=onPlus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}
        Spacer(Modifier.weight(1f))
        Button(onClick=onPost,modifier=Modifier.fillMaxWidth().height(62.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(14.dp)){Icon(Icons.Outlined.Inventory,null,tint=Color.Black);Spacer(Modifier.width(10.dp));Text("Оприходовать на склад",color=Color.Black,fontSize=18.sp,fontWeight=FontWeight.Bold)}
        Text("После проведения остаток увеличится и станет виден клиентам.",color=ProtoMuted,fontSize=12.sp,modifier=Modifier.padding(vertical=12.dp))
    }
}

@Composable
private fun ProtoProductionHistoryScreen(ops:List<ProtoProductionOp>, onBack:()->Unit) {
    val grouped = ops.groupBy { it.date }
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        Row(Modifier.padding(start=16.dp,end=16.dp,top=30.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Column{Text("История приходов",color=ProtoText,fontSize=29.sp,fontWeight=FontWeight.Bold);Text("По датам, артикулам и сборщицам",color=ProtoMuted,fontSize=13.sp)}}
        LazyColumn(modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            grouped.forEach { (date, dayOps) ->
                item(key="h$date"){
                    Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=2.dp),verticalAlignment=Alignment.CenterVertically){Text(date,color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("Итого ${dayOps.sumOf{it.qty}} шт.",color=ProtoMuted)}
                }
                dayOps.forEach { op ->
                    item(key=op.date+op.time+op.sku){
                        Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){
                            Column(Modifier.padding(12.dp)){Row{Text(op.name,color=ProtoText,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Text("${op.qty} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)};Text("Арт. ${op.sku} · ${op.time}",color=ProtoMuted,fontSize=12.sp);Spacer(Modifier.height(5.dp));Row{Icon(Icons.Outlined.Person,null,tint=ProtoGold,modifier=Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("Сборщица: ${op.assembler}",color=ProtoText,fontSize=12.sp);Spacer(Modifier.weight(1f));Text("● ${op.status}",color=ProtoGreen,fontSize=12.sp)};Text("Оприходовал: ${op.postedBy}",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))}
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ProtoCircleBack(onClick:()->Unit){Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xCC11100E)).border(1.dp,ProtoGold,CircleShape).clickable(onClick=onClick),contentAlignment=Alignment.Center){Icon(Icons.Outlined.ArrowBack,null,tint=ProtoGold)}}
@Composable private fun ProtoInfoRow(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text(label,color=ProtoMuted);Spacer(Modifier.weight(1f));Text(value,color=ProtoText)}}
private fun protoToggle(set:Set<String>,value:String)=if(value in set) set-value else set+value
private fun protoMoney(value:Int)=NumberFormat.getCurrencyInstance(Locale("ru","RU")).format(value).replace(",00","")
private fun protoIconForType(type:String)=when(type){"Венки","Венки круглые"->Icons.Outlined.LocalFlorist;"Корзины"->Icons.Outlined.ShoppingBasket;"Полянки"->Icons.Outlined.LocalFlorist;"Флоретки"->Icons.Outlined.Spa;else->Icons.Outlined.Inventory2}

private fun protoLoadProducts(context:Context):List<ProtoCatalogProduct>{
    return try{
        val root=JSONObject(context.assets.open("demo_data.json").bufferedReader().use{it.readText()});val arr=root.getJSONArray("products");
        (0 until arr.length()).map { i -> val o=arr.getJSONObject(i);ProtoCatalogProduct(o.getString("sku"),o.getString("name"),o.getString("type"),o.optString("quality","—"),o.getString("size"),o.getInt("price"),o.getInt("stock"),o.getString("status"),o.optInt("productionDays",3)) }
    }catch(_:Exception){emptyList()}
}

@Composable private fun BoxScope.PrototypeClickArea(maxWidth:Dp,maxHeight:Dp,x:Float,y:Float,w:Float,h:Float,onClick:()->Unit){Box(Modifier.offset(x=maxWidth*x,y=maxHeight*y).width(maxWidth*w).height(maxHeight*h).clickable(onClick=onClick))}
@Composable private fun BoxWithConstraintsScope.protoClientBottomNav(maxWidth:Dp,maxHeight:Dp,go:(ProtoScreen)->Unit,cartCount:Int){
    PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){go(ProtoScreen.Home)}
    PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){go(ProtoScreen.Catalog)}
    PrototypeClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){go(ProtoScreen.Cart)}
    PrototypeClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){go(ProtoScreen.OrderSent)}
    PrototypeClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){go(ProtoScreen.Suspended)}
}
@Composable private fun BoxWithConstraintsScope.protoAdminBottomNav(maxWidth:Dp,maxHeight:Dp,go:(ProtoScreen)->Unit){
    PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){go(ProtoScreen.AdminHome)}
    PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){go(ProtoScreen.ClientCard)}
    PrototypeClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){go(ProtoScreen.OrderSent)}
    PrototypeClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){go(ProtoScreen.Server)}
    PrototypeClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){go(ProtoScreen.AdminHome)}
}
@Composable private fun BoxWithConstraintsScope.protoProductionBottomNav(maxWidth:Dp,maxHeight:Dp,go:(ProtoScreen)->Unit){
    PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){go(ProtoScreen.AdminHome)}
    PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){go(ProtoScreen.Production)}
    PrototypeClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){go(ProtoScreen.Production)}
    PrototypeClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){go(ProtoScreen.Server)}
    PrototypeClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){go(ProtoScreen.Production)}
}
private fun protoDial(context:Context){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${BuildConfig.ADMIN_PHONE}")))}
private fun protoMessage(context:Context){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${BuildConfig.ADMIN_PHONE}")))}
