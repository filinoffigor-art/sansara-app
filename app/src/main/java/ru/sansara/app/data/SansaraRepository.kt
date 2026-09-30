package ru.sansara.app.data

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
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: String = "singleton",
    val payload: String,
    val updatedAt: Long
)

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 'singleton' LIMIT 1")
    suspend fun get(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppStateEntity)
}

@Database(entities = [AppStateEntity::class], version = 1, exportSchema = false)
abstract class SansaraDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
}

class SansaraRepository private constructor(
    private val dao: AppStateDao
) {
    suspend fun load(): SansaraSnapshot? =
        dao.get()?.payload?.let(SansaraSnapshotCodec::decode)

    suspend fun save(snapshot: SansaraSnapshot) {
        dao.upsert(
            AppStateEntity(
                payload = SansaraSnapshotCodec.encode(snapshot),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    companion object {
        fun create(context: Context): SansaraRepository {
            val db = Room.databaseBuilder(
                context.applicationContext,
                SansaraDatabase::class.java,
                "sansara-cache.db"
            ).fallbackToDestructiveMigration().build()
            return SansaraRepository(db.appStateDao())
        }
    }
}

object SansaraSnapshotCodec {
    fun encode(s: SansaraSnapshot): String = JSONObject().apply {
        put("products", JSONArray().apply { s.products.forEach { put(productToJson(it)) } })
        put("clients", JSONArray().apply { s.clients.forEach { put(clientToJson(it)) } })
        put("registrations", JSONArray().apply { s.registrations.forEach { put(registrationToJson(it)) } })
        put("orders", JSONArray().apply { s.orders.forEach { put(orderToJson(it)) } })
        put("productionOps", JSONArray().apply { s.productionOps.forEach { put(productionOpToJson(it)) } })
        put("productionDrafts", JSONArray().apply { s.productionDrafts.forEach { put(productionDraftToJson(it)) } })
        put("stockOverrides", JSONObject().apply { s.stockOverrides.forEach { (k,v) -> put(k,v) } })
        put("cart", JSONObject().apply { s.cart.forEach { (k,v) -> put(k,v) } })
    }.toString()

    fun decode(raw: String): SansaraSnapshot = JSONObject(raw).let { root ->
        SansaraSnapshot(
            products = root.optJSONArray("products").mapObjects(::productFromJson),
            clients = root.optJSONArray("clients").mapObjects(::clientFromJson),
            registrations = root.optJSONArray("registrations").mapObjects(::registrationFromJson),
            orders = root.optJSONArray("orders").mapObjects(::orderFromJson),
            productionOps = root.optJSONArray("productionOps").mapObjects(::productionOpFromJson),
            productionDrafts = root.optJSONArray("productionDrafts").mapObjects(::productionDraftFromJson),
            stockOverrides = root.optJSONObject("stockOverrides").toIntMap(),
            cart = root.optJSONObject("cart").toIntMap()
        )
    }

    private fun productToJson(p: SansaraProduct) = JSONObject().apply {
        put("sku", p.sku); put("name", p.name); put("type", p.type); put("quality", p.quality)
        put("size", p.size); put("price", p.price); put("stock", p.stock); put("status", p.status)
        put("productionDays", p.productionDays); put("imageUrl", p.imageUrl); put("externalId", p.externalId)
    }
    private fun productFromJson(o: JSONObject) = SansaraProduct(
        sku=o.optString("sku"), name=o.optString("name"), type=o.optString("type"),
        quality=o.optString("quality","—"), size=o.optString("size"), price=o.optInt("price"),
        stock=o.optInt("stock"), status=o.optString("status"), productionDays=o.optInt("productionDays",3),
        imageUrl=o.optString("imageUrl"), externalId=o.optString("externalId")
    )

    private fun clientToJson(c: SansaraClient) = JSONObject().apply {
        put("id",c.id); put("name",c.name); put("contact",c.contact); put("phone",c.phone)
        put("status",c.status); put("discount",c.discount); put("monthTurnover",c.monthTurnover)
        put("orderCount",c.orderCount); put("accessCode",c.accessCode); put("online",c.online)
        put("lastSeen",c.lastSeen); put("email",c.email); put("clientType",c.clientType)
        put("registeredAt",c.registeredAt); put("orderingEnabled",c.orderingEnabled)
        put("city",c.city); put("address",c.address); put("firstName",c.firstName)
        put("lastSeenEpochMs",c.lastSeenEpochMs)
    }
    private fun clientFromJson(o: JSONObject) = SansaraClient(
        id=o.optString("id"), name=o.optString("name"), contact=o.optString("contact"),
        phone=o.optString("phone"), status=o.optString("status","Активный"), discount=o.optInt("discount"),
        monthTurnover=o.optInt("monthTurnover"), orderCount=o.optInt("orderCount"),
        accessCode=o.optString("accessCode"), online=o.optBoolean("online"), lastSeen=o.optString("lastSeen"),
        email=o.optString("email"), clientType=o.optString("clientType","Торгующая организация"),
        registeredAt=o.optString("registeredAt"), orderingEnabled=o.optBoolean("orderingEnabled",true),
        city=o.optString("city"), address=o.optString("address"), firstName=o.optString("firstName"),
        lastSeenEpochMs=o.optLong("lastSeenEpochMs",0L)
    )

    private fun registrationToJson(r: SansaraRegistration) = JSONObject().apply {
        put("organization",r.organization); put("fio",r.fio); put("inn",r.inn); put("contact1",r.contact1)
        put("phone1",r.phone1); put("email",r.email); put("city",r.city); put("address",r.address)
        put("type",r.type); put("contact2",r.contact2); put("phone2",r.phone2); put("email2",r.email2)
        put("id",r.id); put("createdAt",r.createdAt); put("status",r.status)
        put("contacts", JSONArray().apply {
            r.contacts.forEach { c ->
                put(JSONObject().apply {
                    put("fullName",c.fullName); put("phone",c.phone); put("email",c.email); put("isPrimary",c.isPrimary)
                })
            }
        })
    }
    private fun registrationFromJson(o: JSONObject): SansaraRegistration {
        val contacts=o.optJSONArray("contacts").mapObjects { c ->
            SansaraContact(c.optString("fullName"),c.optString("phone"),c.optString("email"),c.optBoolean("isPrimary"))
        }
        return SansaraRegistration(
            organization=o.optString("organization"), fio=o.optString("fio"), inn=o.optString("inn"),
            contact1=o.optString("contact1"), phone1=o.optString("phone1"), email=o.optString("email"),
            city=o.optString("city"), address=o.optString("address"), type=o.optString("type"),
            contact2=o.optString("contact2"), phone2=o.optString("phone2"), email2=o.optString("email2"),
            id=o.optString("id"), createdAt=o.optString("createdAt"), status=o.optString("status","Новая"),
            contacts=contacts
        )
    }

    private fun orderToJson(o: SansaraOrder) = JSONObject().apply {
        put("id",o.id); put("clientName",o.clientName); put("dateTime",o.dateTime); put("status",o.status)
        put("deliveryMethod",o.deliveryMethod); put("deliveryAddress",o.deliveryAddress); put("comment",o.comment)
        put("lines", JSONArray().apply { o.lines.forEach { l -> put(JSONObject().apply {
            put("sku",l.sku); put("name",l.name); put("qty",l.qty); put("price",l.price)
        }) } })
        put("history", JSONArray().apply { o.history.forEach { e -> put(JSONObject().apply {
            put("status",e.status); put("dateTime",e.dateTime); put("actor",e.actor)
        }) } })
    }
    private fun orderFromJson(o: JSONObject) = SansaraOrder(
        id=o.optString("id"), clientName=o.optString("clientName"), dateTime=o.optString("dateTime"),
        lines=o.optJSONArray("lines").mapObjects { l ->
            SansaraOrderLine(l.optString("sku"),l.optString("name"),l.optInt("qty"),l.optInt("price"))
        },
        status=o.optString("status"),
        history=o.optJSONArray("history").mapObjects { e ->
            SansaraOrderEvent(e.optString("status"),e.optString("dateTime"),e.optString("actor"))
        },
        deliveryMethod=o.optString("deliveryMethod","Доставка"),
        deliveryAddress=o.optString("deliveryAddress"),
        comment=o.optString("comment")
    )

    private fun productionOpToJson(p: SansaraProductionOp) = JSONObject().apply {
        put("date",p.date); put("time",p.time); put("sku",p.sku); put("name",p.name)
        put("qty",p.qty); put("assembler",p.assembler); put("postedBy",p.postedBy); put("status",p.status)
    }
    private fun productionOpFromJson(o: JSONObject) = SansaraProductionOp(
        date=o.optString("date"), time=o.optString("time"), sku=o.optString("sku"), name=o.optString("name"),
        qty=o.optInt("qty"), assembler=o.optString("assembler"), postedBy=o.optString("postedBy"),
        status=o.optString("status","Проведен")
    )

    private fun productionDraftToJson(d: SansaraProductionDraft) = JSONObject().apply {
        put("product",productToJson(d.product)); put("qty",d.qty); put("assembler",d.assembler); put("date",d.date)
    }
    private fun productionDraftFromJson(o: JSONObject) = SansaraProductionDraft(
        product=productFromJson(o.optJSONObject("product") ?: JSONObject()),
        qty=o.optInt("qty"), assembler=o.optString("assembler"), date=o.optString("date")
    )

    private fun <T> JSONArray?.mapObjects(block:(JSONObject)->T): List<T> {
        if(this==null) return emptyList()
        return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(block) }
    }

    private fun JSONObject?.toIntMap(): Map<String,Int> {
        if(this==null) return emptyMap()
        val result=linkedMapOf<String,Int>()
        keys().forEach { key -> result[key]=optInt(key) }
        return result
    }
}
