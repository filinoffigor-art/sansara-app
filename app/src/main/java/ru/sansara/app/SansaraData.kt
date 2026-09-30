package ru.sansara.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val sku: String,
    val name: String,
    val type: String,
    val quality: String,
    val size: String,
    val price: Int,
    val stock: Int,
    val physicalOverride: Int?,
    val status: String,
    val productionDays: Int,
    val imageUrl: String,
    val externalId: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val firstName: String,
    val contact: String,
    val phone: String,
    val status: String,
    val discount: Int,
    val monthTurnover: Int,
    val orderCount: Int,
    val email: String,
    val clientType: String,
    val registeredAt: String,
    val orderingEnabled: Boolean,
    val city: String,
    val address: String
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val userId: String,
    val clientId: String?,
    val role: String,
    val firstName: String,
    val lastName: String,
    val middleName: String,
    val phone: String,
    val email: String,
    val accessCodeHash: String,
    val accessCodeEncrypted: String,
    val enabled: Boolean = true,
    val lastSeenEpochMs: Long = 0L
)

@Entity(tableName = "registrations")
data class RegistrationEntity(
    @PrimaryKey val id: String,
    val organization: String,
    val fio: String,
    val inn: String,
    val contact1: String,
    val phone1: String,
    val email: String,
    val city: String,
    val address: String,
    val type: String,
    val contact2: String,
    val phone2: String,
    val email2: String,
    val createdAt: String,
    val status: String
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val clientName: String,
    val dateTime: String,
    val linesJson: String,
    val status: String,
    val historyJson: String,
    val deliveryMethod: String,
    val deliveryAddress: String,
    val comment: String
)

@Entity(tableName = "production_ops")
data class ProductionOpEntity(
    @PrimaryKey val opId: String,
    val date: String,
    val time: String,
    val sku: String,
    val name: String,
    val qty: Int,
    val assembler: String,
    val postedBy: String,
    val status: String
)

@Entity(tableName = "cart")
data class CartEntity(@PrimaryKey val sku: String, val qty: Int)

@Dao
interface SansaraDao {
    @Query("SELECT COUNT(*) FROM products")
    suspend fun productCount(): Int

    @Query("SELECT * FROM products ORDER BY sku")
    suspend fun products(): List<ProductEntity>

    @Query("SELECT * FROM clients ORDER BY name")
    suspend fun clients(): List<ClientEntity>

    @Query("SELECT * FROM accounts")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE accessCodeHash = :hash AND enabled = 1 LIMIT 1")
    suspend fun accountByAccessCodeHash(hash: String): AccountEntity?

    @Query("SELECT * FROM registrations ORDER BY createdAt DESC")
    suspend fun registrations(): List<RegistrationEntity>

    @Query("SELECT * FROM orders ORDER BY dateTime DESC")
    suspend fun orders(): List<OrderEntity>

    @Query("SELECT * FROM production_ops ORDER BY date DESC, time DESC")
    suspend fun productionOps(): List<ProductionOpEntity>

    @Query("SELECT * FROM cart")
    suspend fun cart(): List<CartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putClients(items: List<ClientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAccounts(items: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRegistrations(items: List<RegistrationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putOrders(items: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProductionOps(items: List<ProductionOpEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCart(items: List<CartEntity>)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM clients")
    suspend fun clearClients()

    @Query("DELETE FROM registrations")
    suspend fun clearRegistrations()

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM production_ops")
    suspend fun clearProductionOps()

    @Query("DELETE FROM cart")
    suspend fun clearCart()

    @Query("DELETE FROM registrations WHERE id = :id")
    suspend fun deleteRegistration(id: String)

    @Query("UPDATE accounts SET lastSeenEpochMs = :epoch WHERE userId = :userId")
    suspend fun updateLastSeen(userId: String, epoch: Long)
}

@Database(
    entities = [
        ProductEntity::class,
        ClientEntity::class,
        AccountEntity::class,
        RegistrationEntity::class,
        OrderEntity::class,
        ProductionOpEntity::class,
        CartEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SansaraDatabase : RoomDatabase() {
    abstract fun dao(): SansaraDao

    companion object {
        @Volatile private var instance: SansaraDatabase? = null

        fun get(context: Context): SansaraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SansaraDatabase::class.java,
                    "sansara.db"
                ).build().also { instance = it }
            }
    }
}

data class SansaraPersistedSnapshot(
    val products: List<ProtoCatalogProduct>,
    val stockOverrides: Map<String, Int>,
    val clients: List<ProtoClient>,
    val registrations: List<ProtoRegistration>,
    val orders: List<ProtoOrder>,
    val productionOps: List<ProtoProductionOp>,
    val cart: Map<String, Int>
)

data class ApprovalResult(
    val client: ProtoClient,
    val primaryCode: String,
    val secondaryCode: String?
)

class SansaraRepository private constructor(
    private val context: Context,
    private val db: SansaraDatabase,
    private val vault: SansaraVault
) {
    private val dao = db.dao()
    private val random = SecureRandom()

    suspend fun seedDebugIfNeeded() {
        if (!BuildConfig.DEBUG || dao.productCount() > 0) return
        val root = JSONObject(context.assets.open("demo_data.json").bufferedReader().use { it.readText() })
        val array = root.getJSONArray("products")
        val products = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            ProductEntity(
                sku = o.getString("sku"),
                name = o.getString("name"),
                type = o.getString("type"),
                quality = o.optString("quality", "—"),
                size = o.getString("size"),
                price = o.getInt("price"),
                stock = o.getInt("stock"),
                physicalOverride = null,
                status = o.getString("status"),
                productionDays = o.optInt("productionDays", 3),
                imageUrl = o.optString("imageUrl", ""),
                externalId = o.optString("externalId", o.getString("sku"))
            )
        }

        val clients = listOf(
            ClientEntity("C-1024","ООО Ритуал-Сервис","Игорь","Игорь Петров","+7 999 123-45-67","Оптовик",10,286400,7,"info@ritual-service.ru","Торгующая организация","12.03.2023",true,"Москва","г. Москва, ул. Ленинская, д. 10, стр. 2"),
            ClientEntity("C-1025","Агент Смирнов А.А.","Алексей","Алексей Смирнов","+7 916 222-18-44","Активный",5,94800,3,"smirnov@example.ru","Агент","05.09.2026",true,"Тула",""),
            ClientEntity("C-1026","ООО Мемориал","Анна","Анна Сергеева","+7 903 555-34-11","Приостановлен",7,121500,4,"","Торгующая организация","01.09.2026",false,"",""),
            ClientEntity("C-1027","Ритуал-Тула","Сергей","Сергей Орлов","+7 920 701-20-80","VIP",15,418300,11,"","Торгующая организация","03.09.2026",true,"",""),
            ClientEntity("C-1028","Агент Ковалёв","Дмитрий","Дмитрий Ковалёв","+7 905 330-45-21","Приостановлен",5,48200,2,"","Агент","04.09.2026",false,"",""),
            ClientEntity("C-1029","ООО Вечная память","Ольга","Ольга Волкова","+7 977 300-19-50","Приостановлен",10,173900,6,"","Торгующая организация","05.09.2026",false,"","")
        )

        fun account(userId:String,clientId:String?,role:SansaraRole,firstName:String,phone:String,email:String,code:String,lastSeen:Long=0L)=
            AccountEntity(userId,clientId,role.name,firstName,"","",phone,email,hashCode(code),vault.encrypt(code),true,lastSeen)

        val now = System.currentTimeMillis()
        val accounts = listOf(
            account("U-C1024","C-1024",SansaraRole.CLIENT,"Игорь","+7 999 123-45-67","info@ritual-service.ru","1024",now),
            account("U-C1025","C-1025",SansaraRole.CLIENT,"Алексей","+7 916 222-18-44","smirnov@example.ru","1025",now-120000),
            account("U-C1026","C-1026",SansaraRole.CLIENT,"Анна","+7 903 555-34-11","","1026",now-86400000),
            account("U-C1027","C-1027",SansaraRole.CLIENT,"Сергей","+7 920 701-20-80","","1027",now),
            account("U-C1028","C-1028",SansaraRole.CLIENT,"Дмитрий","+7 905 330-45-21","","1028",now-3*86400000L),
            account("U-C1029","C-1029",SansaraRole.CLIENT,"Ольга","+7 977 300-19-50","","1029",now-5*86400000L),
            account("U-ADMIN",null,SansaraRole.ADMIN,"Игорь",BuildConfig.ADMIN_PHONE,"","9001"),
            account("U-PRODUCTION",null,SansaraRole.PRODUCTION,"Производство","+7 900 000-00-02","","9002")
        )

        val registrations = listOf(
            ProtoRegistration("ООО Ритуал-Плюс","Иванов Иван Иванович","7100000001","Иван Иванов","+7 900 111-11-11","ivanov@example.ru","Тула","ул. Ленина, 10","Торгующая организация"),
            ProtoRegistration("Агент Петров","Петров Пётр Петрович","","Пётр Петров","+7 900 222-22-22","petrov@example.ru","Москва","","Агент"),
            ProtoRegistration("ООО Память","Соколова Елена Викторовна","7100000003","Елена Соколова","+7 900 333-33-33","sokolova@example.ru","Калуга","ул. Мира, 5","Торгующая организация")
        )

        val orders = listOf(
            ProtoOrder("S-002384","ООО Ритуал-Сервис","28.09.2026 09:12",listOf(
                ProtoOrderLine("V-060-001","Венок Премиум 60 см №01",5,3750),
                ProtoOrderLine("V-140-005","Венок Эконом 140 см №05",4,2350)
            ),"Собирается",listOf(
                ProtoOrderEvent("Получен","28.09.2026 09:12","Игорь Петров"),
                ProtoOrderEvent("Подтверждён","28.09.2026 09:24","Администратор"),
                ProtoOrderEvent("Собирается","28.09.2026 09:40","Администратор")
            )),
            ProtoOrder("S-002383","Ритуал-Тула","28.09.2026 08:47",listOf(
                ProtoOrderLine("V-060-007","Венок Стандарт 60 см №07",8,2150)
            ),"Подтверждён"),
            ProtoOrder("S-002382","Агент Смирнов А.А.","27.09.2026 16:05",listOf(
                ProtoOrderLine("V-090-011","Венок Премиум 90 см №11",3,3850),
                ProtoOrderLine("KOR-070-004","Корзина 70 см №04",2,2450)
            ),"Доставляется")
        )

        val ops = listOf(
            ProtoProductionOp(today(),"08:12","V-060-001","Венок Премиум 60 см №01",6,"Анна К.","Игорь Ф."),
            ProtoProductionOp(today(),"08:24","V-060-007","Венок Стандарт 60 см №07",8,"Мария С.","Игорь Ф."),
            ProtoProductionOp(today(),"08:41","V-140-005","Венок Эконом 140 см №05",5,"Елена П.","Игорь Ф.")
        )

        db.withTransaction {
            dao.putProducts(products)
            dao.putClients(clients)
            dao.putAccounts(accounts)
            dao.putRegistrations(registrations.map { it.toEntity() })
            dao.putOrders(orders.map { it.toEntity() })
            dao.putProductionOps(ops.mapIndexed { i, item -> item.toEntity("seed-"+i) })
        }
    }

    suspend fun snapshot(): SansaraPersistedSnapshot {
        val accounts = dao.accounts()
        val accountsByClient = accounts.filter { it.clientId != null }.groupBy { it.clientId!! }
        val now = System.currentTimeMillis()
        val clients = dao.clients().map { c ->
            val account = accountsByClient[c.id]?.firstOrNull { it.enabled }
            val code = account?.accessCodeEncrypted?.let(vault::decrypt).orEmpty()
            val lastSeen = account?.lastSeenEpochMs ?: 0L
            ProtoClient(
                id=c.id,name=c.name,contact=c.contact,phone=c.phone,status=c.status,discount=c.discount,
                monthTurnover=c.monthTurnover,orderCount=c.orderCount,accessCode=code,
                online=lastSeen>0 && now-lastSeen<90_000L,lastSeen=lastSeenLabel(lastSeen),
                email=c.email,clientType=c.clientType,registeredAt=c.registeredAt,orderingEnabled=c.orderingEnabled,
                city=c.city,address=c.address,firstName=c.firstName
            )
        }
        val productEntities = dao.products()
        return SansaraPersistedSnapshot(
            products=productEntities.map { it.toProto() },
            stockOverrides=productEntities.mapNotNull { e -> e.physicalOverride?.let { e.sku to it } }.toMap(),
            clients=clients,
            registrations=dao.registrations().map { it.toProto() },
            orders=dao.orders().map { it.toProto() },
            productionOps=dao.productionOps().map { it.toProto() },
            cart=dao.cart().associate { it.sku to it.qty }
        )
    }

    suspend fun persistSnapshot(
        products: List<ProtoCatalogProduct>,
        stockOverrides: Map<String,Int>,
        clients: List<ProtoClient>,
        registrations: List<ProtoRegistration>,
        orders: List<ProtoOrder>,
        productionOps: List<ProtoProductionOp>,
        cart: Map<String,Int>
    ) {
        db.withTransaction {
            dao.clearProducts()
            dao.putProducts(products.map { it.toEntity(stockOverrides[it.sku]) })
            dao.clearClients()
            dao.putClients(clients.map { it.toEntity() })
            dao.clearRegistrations()
            dao.putRegistrations(registrations.map { it.toEntity() })
            dao.clearOrders()
            dao.putOrders(orders.map { it.toEntity() })
            dao.clearProductionOps()
            dao.putProductionOps(productionOps.mapIndexed { i, item -> item.toEntity("op-"+i+"-"+item.date+"-"+item.time+"-"+item.sku) })
            dao.clearCart()
            dao.putCart(cart.map { CartEntity(it.key,it.value) })
        }
    }

    suspend fun authenticate(phone: String, code: String): SansaraSession? {
        val normalizedPhone = normalizePhone(phone)
        val hash = hashCode(code.trim())
        val account = dao.accounts().firstOrNull {
            it.enabled && normalizePhone(it.phone) == normalizedPhone && it.accessCodeHash == hash
        } ?: return null

        if (account.clientId != null) {
            val client = dao.clients().firstOrNull { it.id == account.clientId } ?: return null
            if (client.status == "Приостановлен" || !client.orderingEnabled) return null
        }

        return SansaraSession(
            userId=account.userId,
            clientId=account.clientId,
            role=runCatching { SansaraRole.valueOf(account.role) }.getOrDefault(SansaraRole.CLIENT),
            firstName=account.firstName
        )
    }

    suspend fun validateSession(session: SansaraSession): Boolean {
        val account = dao.accounts().firstOrNull { it.userId == session.userId && it.enabled } ?: return false
        if (account.role != session.role.name) return false
        if (account.clientId != null) {
            val client = dao.clients().firstOrNull { it.id == account.clientId } ?: return false
            if (client.status == "Приостановлен" || !client.orderingEnabled) return false
        }
        return true
    }

    suspend fun updatePresence(userId:String,epoch:Long=System.currentTimeMillis()) {
        dao.updateLastSeen(userId,epoch)
    }

    suspend fun approve(reg: ProtoRegistration): ApprovalResult {
        val primaryCode = nextUniqueCode()
        val secondaryCode = reg.phone2.takeIf { it.isNotBlank() }?.let { nextUniqueCode(exclude=setOf(primaryCode)) }
        val clientId = "C-" + java.util.UUID.randomUUID().toString().replace("-","").take(8).uppercase()
        val firstName = firstName(reg.contact1)
        val client = ProtoClient(
            id=clientId,name=reg.organization,contact=reg.contact1,phone=reg.phone1,status="Активный",
            discount=0,monthTurnover=0,orderCount=0,accessCode=primaryCode,online=false,lastSeen="ещё не входил",
            email=reg.email,clientType=reg.type,registeredAt=today(),orderingEnabled=true,city=reg.city,address=reg.address,
            firstName=firstName
        )
        val accounts = mutableListOf(
            AccountEntity(
                userId="U-"+java.util.UUID.randomUUID().toString().take(8).uppercase(),
                clientId=clientId,role=SansaraRole.CLIENT.name,firstName=firstName,lastName="",middleName="",
                phone=reg.phone1,email=reg.email,accessCodeHash=hashCode(primaryCode),accessCodeEncrypted=vault.encrypt(primaryCode)
            )
        )
        if (secondaryCode != null) {
            accounts += AccountEntity(
                userId="U-"+java.util.UUID.randomUUID().toString().take(8).uppercase(),
                clientId=clientId,role=SansaraRole.CLIENT.name,firstName=firstName(reg.contact2),lastName="",middleName="",
                phone=reg.phone2,email=reg.email2,accessCodeHash=hashCode(secondaryCode),accessCodeEncrypted=vault.encrypt(secondaryCode)
            )
        }
        db.withTransaction {
            dao.putClients(listOf(client.toEntity()))
            dao.putAccounts(accounts)
            dao.deleteRegistration(reg.id)
        }
        return ApprovalResult(client,primaryCode,secondaryCode)
    }

    suspend fun reject(registrationId:String) {
        dao.deleteRegistration(registrationId)
    }

    private suspend fun nextUniqueCode(exclude:Set<String> = emptySet()): String {
        repeat(1000) {
            val value=(100000+random.nextInt(900000)).toString()
            if(value !in exclude && dao.accountByAccessCodeHash(hashCode(value))==null) return value
        }
        error("Не удалось сгенерировать уникальный код доступа")
    }

    private fun hashCode(value:String):String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun normalizePhone(value:String):String =
        value.filter(Char::isDigit).takeLast(10)

    private fun firstName(value:String):String = value.trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { "Партнёр" }
    private fun today():String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    private fun lastSeenLabel(epoch:Long):String {
        if(epoch<=0L) return "ещё не входил"
        val diff=System.currentTimeMillis()-epoch
        return when {
            diff<90_000L -> "сейчас"
            diff<3_600_000L -> (diff/60_000L).coerceAtLeast(1).toString()+" мин назад"
            diff<86_400_000L -> {
                val time=Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalTime()
                "сегодня "+time.format(DateTimeFormatter.ofPattern("HH:mm"))
            }
            else -> {
                val dt=Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDateTime()
                dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            }
        }
    }

    companion object {
        @Volatile private var instance: SansaraRepository? = null
        fun get(context:Context):SansaraRepository =
            instance ?: synchronized(this) {
                instance ?: SansaraRepository(
                    context.applicationContext,
                    SansaraDatabase.get(context),
                    SansaraVault(context.applicationContext)
                ).also { instance=it }
            }
    }
}

private fun ProtoCatalogProduct.toEntity(physicalOverride:Int?)=ProductEntity(
    sku,name,type,quality,size,price,stock,physicalOverride,status,productionDays,imageUrl,externalId
)
private fun ProductEntity.toProto()=ProtoCatalogProduct(sku,name,type,quality,size,price,stock,status,productionDays,imageUrl,externalId)

private fun ProtoClient.toEntity()=ClientEntity(
    id,name,firstName,contact,phone,status,discount,monthTurnover,orderCount,email,clientType,registeredAt,orderingEnabled,city,address
)

private fun ProtoRegistration.toEntity()=RegistrationEntity(
    id,organization,fio,inn,contact1,phone1,email,city,address,type,contact2,phone2,email2,createdAt,status
)
private fun RegistrationEntity.toProto()=ProtoRegistration(
    organization,fio,inn,contact1,phone1,email,city,address,type,contact2,phone2,email2,id,createdAt,status
)

private fun ProtoOrder.toEntity():OrderEntity {
    val lines=JSONArray().apply { this@toEntity.lines.forEach { line -> put(JSONObject().apply {
        put("sku",line.sku);put("name",line.name);put("qty",line.qty);put("price",line.price)
    }) } }
    val events=JSONArray().apply { this@toEntity.history.forEach { event -> put(JSONObject().apply {
        put("status",event.status);put("dateTime",event.dateTime);put("actor",event.actor)
    }) } }
    return OrderEntity(id,clientName,dateTime,lines.toString(),status,events.toString(),deliveryMethod,deliveryAddress,comment)
}
private fun OrderEntity.toProto():ProtoOrder {
    val lineArray=JSONArray(linesJson)
    val lines=(0 until lineArray.length()).map { i -> lineArray.getJSONObject(i).let {
        ProtoOrderLine(it.getString("sku"),it.getString("name"),it.getInt("qty"),it.getInt("price"))
    } }
    val eventArray=JSONArray(historyJson)
    val events=(0 until eventArray.length()).map { i -> eventArray.getJSONObject(i).let {
        ProtoOrderEvent(it.getString("status"),it.getString("dateTime"),it.getString("actor"))
    } }
    return ProtoOrder(id,clientName,dateTime,lines,status,events,deliveryMethod,deliveryAddress,comment)
}

private fun ProtoProductionOp.toEntity(id:String)=ProductionOpEntity(id,date,time,sku,name,qty,assembler,postedBy,status)
private fun ProductionOpEntity.toProto()=ProtoProductionOp(date,time,sku,name,qty,assembler,postedBy,status)
