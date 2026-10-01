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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "assemblers")
data class AssemblerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "production_rates")
data class ProductionRateEntity(
    @PrimaryKey val sku: String,
    val rateRub: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "production_receipts")
data class ProductionReceiptEntity(
    @PrimaryKey val documentId: String,
    val date: String,
    val time: String,
    val userId: String,
    val totalQty: Int,
    val totalAmount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val reversedByDocumentId: String? = null
)

@Entity(tableName = "production_receipt_lines")
data class ProductionReceiptLineEntity(
    @PrimaryKey val lineId: String,
    val documentId: String,
    val opId: String,
    val sku: String,
    val name: String,
    val assemblerId: String,
    val assemblerName: String,
    val qty: Int,
    val rateRub: Int,
    val amountRub: Int
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustmentEntity(
    @PrimaryKey val adjustmentId: String,
    val sku: String,
    val qtyDelta: Int,
    val reason: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderRole: String,
    val senderId: String,
    val body: String,
    val attachmentUri: String,
    val attachmentName: String,
    val attachmentMime: String,
    val createdAt: Long,
    val read: Boolean = false
)

@Entity(tableName = "agent_customers")
data class AgentCustomerEntity(
    @PrimaryKey val id: String,
    val ownerClientId: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val city: String,
    val address: String,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_settings")
data class AgentSettingsEntity(
    @PrimaryKey val ownerClientId: String,
    val generalMarkupEnabled: Boolean,
    val generalMarkupPct: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_customer_markups", primaryKeys = ["customerId", "category"])
data class AgentCustomerMarkupEntity(
    val customerId: String,
    val ownerClientId: String,
    val category: String,
    val markupPct: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_reminders")
data class AgentReminderEntity(
    @PrimaryKey val id: String,
    val ownerClientId: String,
    val customerId: String,
    val customerName: String,
    val remindAtEpochMs: Long,
    val note: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_controls")
data class AdminControlEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val isMain: Boolean,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workshop_tasks")
data class WorkshopTaskEntity(
    @PrimaryKey val id: String,
    val taskDate: String,
    val linesJson: String,
    val comment: String,
    val commentOnly: Boolean,
    val status: String,
    val actualQty: Int,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance", primaryKeys = ["date", "personId"])
data class AttendanceEntity(
    val date: String,
    val personId: String,
    val personName: String,
    val status: String,
    val comment: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "production_audit")
data class ProductionAuditEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val opId: String,
    val action: String,
    val oldValue: String,
    val newValue: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "presence_sessions")
data class PresenceSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val clientId: String?,
    val role: String,
    val day: String,
    val startedAt: Long,
    val lastSeenAt: Long,
    val durationMs: Long
)

@Entity(tableName = "admin_daily_status")
data class AdminDailyStatusEntity(
    @PrimaryKey val date: String,
    val dayOff: Boolean,
    val reason: String,
    val updatedAt: Long = System.currentTimeMillis()
)

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

    @Query("SELECT * FROM accounts WHERE userId = :userId LIMIT 1")
    suspend fun accountByUserId(userId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE role = 'ADMIN' ORDER BY firstName")
    suspend fun adminAccounts(): List<AccountEntity>

    @Query("UPDATE accounts SET enabled = :enabled WHERE userId = :userId")
    suspend fun setAccountEnabled(userId: String, enabled: Boolean)

    @Query("SELECT * FROM accounts WHERE clientId = :clientId AND enabled = 1 ORDER BY firstName, lastName")
    suspend fun accountsForClient(clientId: String): List<AccountEntity>

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

    @Query("SELECT COUNT(*) FROM assemblers")
    suspend fun assemblerCount(): Int

    @Query("SELECT * FROM assemblers ORDER BY enabled DESC, name")
    suspend fun assemblers(): List<AssemblerEntity>

    @Query("SELECT * FROM assemblers WHERE enabled = 1 ORDER BY name")
    suspend fun activeAssemblers(): List<AssemblerEntity>

    @Query("SELECT * FROM production_rates")
    suspend fun productionRates(): List<ProductionRateEntity>

    @Query("SELECT * FROM production_receipt_lines")
    suspend fun productionReceiptLines(): List<ProductionReceiptLineEntity>

    @Query("SELECT * FROM production_receipts ORDER BY createdAt DESC")
    suspend fun productionReceipts(): List<ProductionReceiptEntity>

    @Query("SELECT * FROM production_receipts WHERE documentId = :documentId LIMIT 1")
    suspend fun productionReceipt(documentId: String): ProductionReceiptEntity?

    @Query("SELECT * FROM production_receipt_lines WHERE documentId = :documentId")
    suspend fun productionReceiptLines(documentId: String): List<ProductionReceiptLineEntity>

    @Query("SELECT * FROM production_receipt_lines WHERE opId = :opId LIMIT 1")
    suspend fun productionReceiptLineByOp(opId: String): ProductionReceiptLineEntity?

    @Query("SELECT * FROM production_ops WHERE opId = :opId LIMIT 1")
    suspend fun productionOpById(opId: String): ProductionOpEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun productBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    suspend fun chatMessages(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun chatMessages(conversationId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putChatMessages(items: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET read = 1 WHERE conversationId = :conversationId AND senderRole != :readerRole")
    suspend fun markConversationRead(conversationId: String, readerRole: String)

    @Query("SELECT * FROM agent_customers ORDER BY createdAt DESC")
    suspend fun allAgentCustomers(): List<AgentCustomerEntity>

    @Query("SELECT * FROM agent_customers WHERE ownerClientId = :ownerClientId ORDER BY createdAt DESC")
    suspend fun agentCustomers(ownerClientId: String): List<AgentCustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAgentCustomers(items: List<AgentCustomerEntity>)

    @Query("SELECT * FROM agent_settings WHERE ownerClientId = :ownerClientId LIMIT 1")
    suspend fun agentSettings(ownerClientId: String): AgentSettingsEntity?

    @Query("SELECT * FROM agent_settings")
    suspend fun allAgentSettings(): List<AgentSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAgentSettings(items: List<AgentSettingsEntity>)

    @Query("SELECT * FROM agent_customer_markups")
    suspend fun allAgentCustomerMarkups(): List<AgentCustomerMarkupEntity>

    @Query("SELECT * FROM agent_customer_markups WHERE customerId = :customerId ORDER BY category")
    suspend fun agentCustomerMarkups(customerId: String): List<AgentCustomerMarkupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAgentCustomerMarkups(items: List<AgentCustomerMarkupEntity>)

    @Query("SELECT * FROM agent_reminders WHERE active = 1 ORDER BY remindAtEpochMs")
    suspend fun activeAgentReminders(): List<AgentReminderEntity>

    @Query("SELECT * FROM agent_reminders WHERE ownerClientId = :ownerClientId ORDER BY remindAtEpochMs DESC")
    suspend fun agentReminders(ownerClientId: String): List<AgentReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAgentReminders(items: List<AgentReminderEntity>)

    @Query("UPDATE agent_reminders SET active = 0 WHERE id = :id")
    suspend fun disableAgentReminder(id: String)

    @Query("SELECT COUNT(*) FROM admin_controls")
    suspend fun adminControlCount(): Int

    @Query("SELECT * FROM admin_controls ORDER BY isMain DESC, displayName")
    suspend fun adminControls(): List<AdminControlEntity>

    @Query("SELECT * FROM admin_controls WHERE userId = :userId LIMIT 1")
    suspend fun adminControl(userId: String): AdminControlEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAdminControls(items: List<AdminControlEntity>)

    @Query("SELECT * FROM workshop_tasks ORDER BY createdAt DESC")
    suspend fun workshopTasks(): List<WorkshopTaskEntity>

    @Query("SELECT * FROM workshop_tasks WHERE taskDate = :date ORDER BY createdAt DESC")
    suspend fun workshopTasks(date: String): List<WorkshopTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putWorkshopTasks(items: List<WorkshopTaskEntity>)

    @Query("SELECT * FROM attendance ORDER BY date DESC, personName")
    suspend fun allAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY personName")
    suspend fun attendance(date: String): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAttendance(items: List<AttendanceEntity>)

    @Query("SELECT * FROM production_audit ORDER BY createdAt DESC")
    suspend fun productionAudit(): List<ProductionAuditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProductionAudit(items: List<ProductionAuditEntity>)

    @Query("SELECT * FROM presence_sessions WHERE day = :day ORDER BY lastSeenAt DESC")
    suspend fun presenceSessions(day: String): List<PresenceSessionEntity>

    @Query("SELECT * FROM presence_sessions WHERE id = :id LIMIT 1")
    suspend fun presenceSession(id: String): PresenceSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPresenceSessions(items: List<PresenceSessionEntity>)

    @Query("SELECT * FROM admin_daily_status WHERE date = :date LIMIT 1")
    suspend fun adminDailyStatus(date: String): AdminDailyStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAdminDailyStatus(items: List<AdminDailyStatusEntity>)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAssemblers(items: List<AssemblerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProductionRates(items: List<ProductionRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProductionReceipts(items: List<ProductionReceiptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProductionReceiptLines(items: List<ProductionReceiptLineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putStockAdjustments(items: List<StockAdjustmentEntity>)

    @Query("UPDATE assemblers SET enabled = :enabled WHERE id = :id")
    suspend fun setAssemblerEnabled(id: String, enabled: Boolean)

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
        CartEntity::class,
        AssemblerEntity::class,
        ProductionRateEntity::class,
        ProductionReceiptEntity::class,
        ProductionReceiptLineEntity::class,
        StockAdjustmentEntity::class,
        ChatMessageEntity::class,
        AgentCustomerEntity::class,
        AgentSettingsEntity::class,
        AgentCustomerMarkupEntity::class,
        AgentReminderEntity::class,
        AdminControlEntity::class,
        WorkshopTaskEntity::class,
        AttendanceEntity::class,
        ProductionAuditEntity::class,
        PresenceSessionEntity::class,
        AdminDailyStatusEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SansaraDatabase : RoomDatabase() {
    abstract fun dao(): SansaraDao

    companion object {
        @Volatile private var instance: SansaraDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS assemblers (id TEXT NOT NULL, name TEXT NOT NULL, enabled INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS production_rates (sku TEXT NOT NULL, rateRub INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(sku))")
                db.execSQL("CREATE TABLE IF NOT EXISTS production_receipts (documentId TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, userId TEXT NOT NULL, totalQty INTEGER NOT NULL, totalAmount INTEGER NOT NULL, createdAt INTEGER NOT NULL, reversedByDocumentId TEXT, PRIMARY KEY(documentId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS production_receipt_lines (lineId TEXT NOT NULL, documentId TEXT NOT NULL, opId TEXT NOT NULL, sku TEXT NOT NULL, name TEXT NOT NULL, assemblerId TEXT NOT NULL, assemblerName TEXT NOT NULL, qty INTEGER NOT NULL, rateRub INTEGER NOT NULL, amountRub INTEGER NOT NULL, PRIMARY KEY(lineId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS stock_adjustments (adjustmentId TEXT NOT NULL, sku TEXT NOT NULL, qtyDelta INTEGER NOT NULL, reason TEXT NOT NULL, userId TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(adjustmentId))")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id TEXT NOT NULL, conversationId TEXT NOT NULL, senderRole TEXT NOT NULL, senderId TEXT NOT NULL, body TEXT NOT NULL, attachmentUri TEXT NOT NULL, attachmentName TEXT NOT NULL, attachmentMime TEXT NOT NULL, createdAt INTEGER NOT NULL, read INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversationId ON chat_messages(conversationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_createdAt ON chat_messages(createdAt)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS agent_customers (id TEXT NOT NULL, ownerClientId TEXT NOT NULL, fullName TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, city TEXT NOT NULL, address TEXT NOT NULL, comment TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_customers_ownerClientId ON agent_customers(ownerClientId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS agent_settings (ownerClientId TEXT NOT NULL, generalMarkupEnabled INTEGER NOT NULL, generalMarkupPct INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(ownerClientId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS agent_customer_markups (customerId TEXT NOT NULL, ownerClientId TEXT NOT NULL, category TEXT NOT NULL, markupPct INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(customerId, category))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_customer_markups_ownerClientId ON agent_customer_markups(ownerClientId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS agent_reminders (id TEXT NOT NULL, ownerClientId TEXT NOT NULL, customerId TEXT NOT NULL, customerName TEXT NOT NULL, remindAtEpochMs INTEGER NOT NULL, note TEXT NOT NULL, active INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_reminders_ownerClientId ON agent_reminders(ownerClientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_reminders_remindAtEpochMs ON agent_reminders(remindAtEpochMs)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS admin_controls (userId TEXT NOT NULL, displayName TEXT NOT NULL, isMain INTEGER NOT NULL, enabled INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(userId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS workshop_tasks (id TEXT NOT NULL, taskDate TEXT NOT NULL, linesJson TEXT NOT NULL, comment TEXT NOT NULL, commentOnly INTEGER NOT NULL, status TEXT NOT NULL, actualQty INTEGER NOT NULL, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS attendance (date TEXT NOT NULL, personId TEXT NOT NULL, personName TEXT NOT NULL, status TEXT NOT NULL, comment TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(date, personId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS production_audit (id TEXT NOT NULL, documentId TEXT NOT NULL, opId TEXT NOT NULL, action TEXT NOT NULL, oldValue TEXT NOT NULL, newValue TEXT NOT NULL, userId TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS presence_sessions (id TEXT NOT NULL, userId TEXT NOT NULL, clientId TEXT, role TEXT NOT NULL, day TEXT NOT NULL, startedAt INTEGER NOT NULL, lastSeenAt INTEGER NOT NULL, durationMs INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS admin_daily_status (date TEXT NOT NULL, dayOff INTEGER NOT NULL, reason TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(date))")
            }
        }

        fun get(context: Context): SansaraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SansaraDatabase::class.java,
                    "sansara.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
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
    val cart: Map<String, Int>,
    val assemblers: List<SansaraAssembler>,
    val productionRates: Map<String, Int>
)

data class ApprovalResult(
    val client: ProtoClient,
    val primaryCode: String,
    val secondaryCode: String?
)

data class SansaraCompanyContact(
    val userId: String,
    val name: String,
    val phone: String,
    val email: String
)

data class SansaraAssembler(
    val id: String,
    val name: String,
    val enabled: Boolean
)

data class ProductionPostingLine(
    val sku: String,
    val name: String,
    val assemblerId: String,
    val assemblerName: String,
    val qty: Int,
    val rateRub: Int
)

data class ProductionReceiptResult(
    val documentId: String,
    val totalQty: Int,
    val totalAmount: Int
)

data class SansaraChatMessage(
    val id: String,
    val conversationId: String,
    val senderRole: String,
    val senderId: String,
    val body: String,
    val attachmentUri: String,
    val attachmentName: String,
    val attachmentMime: String,
    val createdAt: Long,
    val read: Boolean
)

data class SansaraAgentCustomer(
    val id: String,
    val ownerClientId: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val city: String,
    val address: String,
    val comment: String,
    val createdAt: Long
)

data class SansaraAgentSettings(
    val ownerClientId: String,
    val generalMarkupEnabled: Boolean,
    val generalMarkupPct: Int
)

data class SansaraAgentMarkup(
    val customerId: String,
    val ownerClientId: String,
    val category: String,
    val markupPct: Int
)

data class SansaraAgentReminder(
    val id: String,
    val ownerClientId: String,
    val customerId: String,
    val customerName: String,
    val remindAtEpochMs: Long,
    val note: String,
    val active: Boolean
)

data class SansaraAdminAccount(
    val userId: String,
    val displayName: String,
    val isMain: Boolean,
    val enabled: Boolean
)

class SansaraRepository private constructor(
    private val context: Context,
    private val db: SansaraDatabase,
    private val vault: SansaraVault
) {
    private val dao = db.dao()
    private val random = SecureRandom()

    suspend fun seedDebugIfNeeded() {
        if (!BuildConfig.DEBUG) return
        if (dao.productCount() > 0) {
            if (dao.adminControlCount() == 0) {
                dao.adminAccounts().firstOrNull()?.let { admin ->
                    dao.putAdminControls(listOf(AdminControlEntity(admin.userId,admin.firstName.ifBlank{"Главный администратор"},true,admin.enabled)))
                }
            }
            if (dao.assemblerCount() == 0) {
                dao.putAssemblers(
                    listOf(
                        AssemblerEntity("ASM-001","Анна К."),
                        AssemblerEntity("ASM-002","Мария С."),
                        AssemblerEntity("ASM-003","Елена П.")
                    )
                )
            }
            return
        }
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
            dao.putAdminControls(listOf(AdminControlEntity("U-ADMIN","Игорь",true,true)))
            dao.putRegistrations(registrations.map { it.toEntity() })
            dao.putOrders(orders.map { it.toEntity() })
            dao.putProductionOps(ops.mapIndexed { i, item -> item.toEntity("seed-"+i) })
            dao.putAssemblers(
                listOf(
                    AssemblerEntity("ASM-001","Анна К."),
                    AssemblerEntity("ASM-002","Мария С."),
                    AssemblerEntity("ASM-003","Елена П.")
                )
            )
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
        val receiptLinesByOp = dao.productionReceiptLines().associateBy { it.opId }
        val rates = dao.productionRates().associate { it.sku to it.rateRub }
        return SansaraPersistedSnapshot(
            products=productEntities.map { it.toProto() },
            stockOverrides=productEntities.mapNotNull { e -> e.physicalOverride?.let { e.sku to it } }.toMap(),
            clients=clients,
            registrations=dao.registrations().map { it.toProto() },
            orders=dao.orders().map { it.toProto() },
            productionOps=dao.productionOps().map { op ->
                val meta = receiptLinesByOp[op.opId]
                op.toProto(
                    rateRub = meta?.rateRub ?: 0,
                    amountRub = meta?.amountRub ?: 0,
                    documentId = meta?.documentId.orEmpty()
                )
            },
            cart=dao.cart().associate { it.sku to it.qty },
            assemblers=dao.assemblers().map { SansaraAssembler(it.id,it.name,it.enabled) },
            productionRates=rates
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

    suspend fun contactsForClient(clientId: String): List<SansaraCompanyContact> =
        dao.accountsForClient(clientId).map { account ->
            val fullName = listOf(account.lastName, account.firstName, account.middleName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { account.firstName.ifBlank { "Контактное лицо" } }
            SansaraCompanyContact(account.userId, fullName, account.phone, account.email)
        }

    suspend fun nextOrderId(): String {
        val max = dao.orders()
            .mapNotNull { it.id.removePrefix("S-").toIntOrNull() }
            .maxOrNull() ?: 2383
        return "S-" + (max + 1).toString().padStart(6, '0')
    }

    suspend fun saveAssembler(id:String?, name:String): SansaraAssembler {
        val clean=name.trim()
        require(clean.isNotBlank()) { "Имя сборщицы не заполнено" }
        val entity=AssemblerEntity(
            id ?: ("ASM-"+java.util.UUID.randomUUID().toString().replace("-","").take(8).uppercase()),
            clean,
            true
        )
        dao.putAssemblers(listOf(entity))
        return SansaraAssembler(entity.id,entity.name,entity.enabled)
    }

    suspend fun setAssemblerEnabled(id:String,enabled:Boolean) {
        dao.setAssemblerEnabled(id,enabled)
    }

    suspend fun setProductionRate(sku:String,rateRub:Int) {
        dao.putProductionRates(listOf(ProductionRateEntity(sku,rateRub.coerceAtLeast(0))))
    }

    suspend fun postProductionReceipt(
        date:String,
        userId:String,
        lines:List<ProductionPostingLine>
    ): ProductionReceiptResult {
        require(lines.isNotEmpty()) { "Выпуск пуст" }
        val documentId="PR-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase()
        val time=java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val receiptLines=mutableListOf<ProductionReceiptLineEntity>()
        val ops=mutableListOf<ProductionOpEntity>()
        val productsToUpdate=mutableListOf<ProductEntity>()
        db.withTransaction {
            lines.forEach { line ->
                require(line.qty>0) { "Количество должно быть больше нуля" }
                val product=dao.productBySku(line.sku) ?: error("Товар " + line.sku + " не найден")
                val physical=product.physicalOverride ?: product.stock
                productsToUpdate += product.copy(physicalOverride=physical+line.qty,updatedAt=System.currentTimeMillis())
                val opId="OP-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase()
                ops += ProductionOpEntity(opId,date,time,line.sku,line.name,line.qty,line.assemblerName,userId,"Проведен")
                receiptLines += ProductionReceiptLineEntity(
                    lineId="RL-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase(),
                    documentId=documentId,
                    opId=opId,
                    sku=line.sku,
                    name=line.name,
                    assemblerId=line.assemblerId,
                    assemblerName=line.assemblerName,
                    qty=line.qty,
                    rateRub=line.rateRub.coerceAtLeast(0),
                    amountRub=line.qty*line.rateRub.coerceAtLeast(0)
                )
                dao.putProductionRates(listOf(ProductionRateEntity(line.sku,line.rateRub.coerceAtLeast(0))))
            }
            dao.putProducts(productsToUpdate)
            dao.putProductionOps(ops)
            dao.putProductionReceiptLines(receiptLines)
            dao.putProductionReceipts(
                listOf(
                    ProductionReceiptEntity(
                        documentId=documentId,
                        date=date,
                        time=time,
                        userId=userId,
                        totalQty=receiptLines.sumOf { it.qty },
                        totalAmount=receiptLines.sumOf { it.amountRub }
                    )
                )
            )
        }
        return ProductionReceiptResult(documentId,receiptLines.sumOf { it.qty },receiptLines.sumOf { it.amountRub })
    }

    suspend fun chatMessages(conversationId:String):List<SansaraChatMessage> =
        dao.chatMessages(conversationId).map { it.toModel() }

    suspend fun allChatMessages():List<SansaraChatMessage> =
        dao.chatMessages().map { it.toModel() }

    suspend fun sendChatMessage(
        conversationId:String,
        senderRole:String,
        senderId:String,
        body:String,
        attachmentUri:String="",
        attachmentName:String="",
        attachmentMime:String=""
    ):SansaraChatMessage {
        require(body.isNotBlank() || attachmentUri.isNotBlank()) { "Сообщение пустое" }
        val entity=ChatMessageEntity(
            id="MSG-"+java.util.UUID.randomUUID().toString().replace("-","").take(12).uppercase(),
            conversationId=conversationId,
            senderRole=senderRole,
            senderId=senderId,
            body=body.trim(),
            attachmentUri=attachmentUri,
            attachmentName=attachmentName,
            attachmentMime=attachmentMime,
            createdAt=System.currentTimeMillis(),
            read=false
        )
        dao.putChatMessages(listOf(entity))
        return entity.toModel()
    }

    suspend fun markConversationRead(conversationId:String,readerRole:String) {
        dao.markConversationRead(conversationId,readerRole)
    }

    suspend fun allAgentCustomers():List<SansaraAgentCustomer> =
        dao.allAgentCustomers().map { it.toModel() }

    suspend fun agentCustomers(ownerClientId:String):List<SansaraAgentCustomer> =
        dao.agentCustomers(ownerClientId).map { it.toModel() }

    suspend fun saveAgentCustomer(
        id:String?,
        ownerClientId:String,
        fullName:String,
        phone:String,
        email:String,
        city:String,
        address:String,
        comment:String
    ):SansaraAgentCustomer {
        require(fullName.trim().isNotBlank()) { "Заполните ФИО клиента" }
        require(phone.trim().isNotBlank()) { "Заполните телефон клиента" }
        val entity=AgentCustomerEntity(
            id=id ?: ("AC-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase()),
            ownerClientId=ownerClientId,
            fullName=fullName.trim(),
            phone=phone.trim(),
            email=email.trim(),
            city=city.trim(),
            address=address.trim(),
            comment=comment.trim()
        )
        dao.putAgentCustomers(listOf(entity))
        return entity.toModel()
    }

    suspend fun allAgentSettings():List<SansaraAgentSettings> =
        dao.allAgentSettings().map { it.toModel() }

    suspend fun agentSettings(ownerClientId:String):SansaraAgentSettings =
        (dao.agentSettings(ownerClientId) ?: AgentSettingsEntity(ownerClientId,true,30)).toModel()

    suspend fun saveAgentSettings(ownerClientId:String,enabled:Boolean,pct:Int):SansaraAgentSettings {
        val entity=AgentSettingsEntity(ownerClientId,enabled,pct.coerceIn(0,300))
        dao.putAgentSettings(listOf(entity))
        return entity.toModel()
    }

    suspend fun allAgentMarkups():List<SansaraAgentMarkup> =
        dao.allAgentCustomerMarkups().map { it.toModel() }

    suspend fun agentCustomerMarkups(customerId:String):List<SansaraAgentMarkup> =
        dao.agentCustomerMarkups(customerId).map { it.toModel() }

    suspend fun setAgentCustomerMarkup(customerId:String,ownerClientId:String,category:String,pct:Int):SansaraAgentMarkup {
        val entity=AgentCustomerMarkupEntity(customerId,ownerClientId,category,pct.coerceIn(0,300))
        dao.putAgentCustomerMarkups(listOf(entity))
        return entity.toModel()
    }

    suspend fun activeAgentReminders():List<SansaraAgentReminder> =
        dao.activeAgentReminders().map { it.toModel() }

    suspend fun agentReminders(ownerClientId:String):List<SansaraAgentReminder> =
        dao.agentReminders(ownerClientId).map { it.toModel() }

    suspend fun saveAgentReminder(
        ownerClientId:String,
        customerId:String,
        customerName:String,
        remindAtEpochMs:Long,
        note:String
    ):SansaraAgentReminder {
        require(remindAtEpochMs>System.currentTimeMillis()) { "Время напоминания должно быть в будущем" }
        val entity=AgentReminderEntity(
            id="REM-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase(),
            ownerClientId=ownerClientId,
            customerId=customerId,
            customerName=customerName,
            remindAtEpochMs=remindAtEpochMs,
            note=note.trim()
        )
        dao.putAgentReminders(listOf(entity))
        return entity.toModel()
    }

    suspend fun disableAgentReminder(id:String) {
        dao.disableAgentReminder(id)
    }

    suspend fun adjustStock(sku:String,qtyDelta:Int,reason:String,userId:String) {
        require(qtyDelta!=0) { "Корректировка равна нулю" }
        require(reason.trim().isNotBlank()) { "Укажите причину" }
        db.withTransaction {
            val product=dao.productBySku(sku) ?: error("Товар не найден")
            val physical=product.physicalOverride ?: product.stock
            dao.putProducts(
                listOf(
                    product.copy(
                        physicalOverride=(physical+qtyDelta).coerceAtLeast(0),
                        updatedAt=System.currentTimeMillis()
                    )
                )
            )
            dao.putStockAdjustments(
                listOf(
                    StockAdjustmentEntity(
                        adjustmentId="ADJ-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase(),
                        sku=sku,
                        qtyDelta=qtyDelta,
                        reason=reason.trim(),
                        userId=userId
                    )
                )
            )
        }
    }

    suspend fun authenticate(code: String): SansaraSession? {
        val account = dao.accountByAccessCodeHash(hashCode(code.trim())) ?: return null
        return SansaraSession(
            userId=account.userId,
            clientId=account.clientId,
            role=runCatching { SansaraRole.valueOf(account.role) }.getOrDefault(SansaraRole.CLIENT),
            firstName=account.firstName
        )
    }

    suspend fun updatePresence(userId:String,clientId:String?=null,role:String="",epoch:Long=System.currentTimeMillis()) {
        dao.updateLastSeen(userId,epoch)
        val day=LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val id=day+"_"+userId
        val current=dao.presenceSession(id)
        val delta=if(current==null)0L else (epoch-current.lastSeenAt).coerceIn(0L,120_000L)
        dao.putPresenceSessions(
            listOf(
                PresenceSessionEntity(
                    id=id,userId=userId,clientId=clientId,role=role.ifBlank{current?.role.orEmpty()},day=day,
                    startedAt=current?.startedAt?:epoch,lastSeenAt=epoch,durationMs=(current?.durationMs?:0L)+delta
                )
            )
        )
    }

    suspend fun adminAccounts():List<SansaraAdminAccount> {
        val controls=dao.adminControls().associateBy{it.userId}
        return dao.adminAccounts().map { account ->
            val control=controls[account.userId]
            SansaraAdminAccount(account.userId,control?.displayName?:account.firstName,control?.isMain==true,account.enabled)
        }.sortedWith(compareByDescending<SansaraAdminAccount>{it.isMain}.thenBy{it.displayName})
    }

    suspend fun addAdmin(displayName:String,sourceAdminUserId:String):SansaraAdminAccount {
        val actor=dao.adminControl(sourceAdminUserId)
        require(actor?.isMain==true) { "Добавлять администраторов может только главный аккаунт" }
        val source=dao.accountByUserId(sourceAdminUserId) ?: error("Главный администратор не найден")
        val name=displayName.trim()
        require(name.isNotBlank()) { "Введите имя администратора" }
        val id="U-ADM-"+java.util.UUID.randomUUID().toString().replace("-","").take(8).uppercase()
        val account=AccountEntity(id,null,SansaraRole.ADMIN.name,name,"","",BuildConfig.ADMIN_PHONE,"",source.accessCodeHash,source.accessCodeEncrypted,true,0L)
        db.withTransaction {
            dao.putAccounts(listOf(account))
            dao.putAdminControls(listOf(AdminControlEntity(id,name,false,true)))
        }
        return SansaraAdminAccount(id,name,false,true)
    }

    suspend fun setAdminEnabled(targetUserId:String,enabled:Boolean,actingUserId:String) {
        val actor=dao.adminControl(actingUserId)
        require(actor?.isMain==true) { "Отключать администраторов может только главный аккаунт" }
        require(targetUserId!=actingUserId) { "Главный аккаунт нельзя отключить из самого приложения" }
        dao.setAccountEnabled(targetUserId,enabled)
        dao.adminControl(targetUserId)?.let { dao.putAdminControls(listOf(it.copy(enabled=enabled))) }
    }

    suspend fun workshopTasks():List<WorkshopTaskEntity> = dao.workshopTasks()

    suspend fun createWorkshopTask(taskDate:String,linesJson:String,comment:String,commentOnly:Boolean,createdBy:String):WorkshopTaskEntity {
        require(commentOnly || linesJson.isNotBlank()) { "Добавьте позиции задания" }
        require(!commentOnly || comment.trim().isNotBlank()) { "Введите комментарий" }
        val task=WorkshopTaskEntity(
            id="WT-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase(),
            taskDate=taskDate,linesJson=linesJson,comment=comment.trim(),commentOnly=commentOnly,status="Получено",
            actualQty=0,createdBy=createdBy
        )
        dao.putWorkshopTasks(listOf(task))
        return task
    }

    suspend fun updateWorkshopTaskFact(id:String,actualQty:Int,status:String) {
        val task=dao.workshopTasks().firstOrNull{it.id==id} ?: error("Задание не найдено")
        dao.putWorkshopTasks(listOf(task.copy(actualQty=actualQty.coerceAtLeast(0),status=status,updatedAt=System.currentTimeMillis())))
    }

    suspend fun attendance(date:String):List<AttendanceEntity> = dao.attendance(date)
    suspend fun allAttendance():List<AttendanceEntity> = dao.allAttendance()

    suspend fun saveAttendance(date:String,personId:String,personName:String,status:String,comment:String) {
        dao.putAttendance(listOf(AttendanceEntity(date,personId,personName,status,comment.trim())))
    }

    suspend fun productionAudit():List<ProductionAuditEntity> = dao.productionAudit()

    suspend fun changeProductionOp(opId:String,newQty:Int,userId:String,delete:Boolean=false) {
        val op=dao.productionOpById(opId) ?: error("Выпуск не найден")
        val line=dao.productionReceiptLineByOp(opId) ?: error("Строка прихода не найдена")
        val product=dao.productBySku(op.sku) ?: error("Товар не найден")
        val targetQty=if(delete)0 else newQty.coerceAtLeast(1)
        val delta=targetQty-op.qty
        val physical=product.physicalOverride?:product.stock
        require(physical+delta>=0) { "Недостаточно остатка для корректировки" }
        val receipt=dao.productionReceipt(line.documentId)
        val oldJson=JSONObject().put("qty",op.qty).put("status",op.status).toString()
        db.withTransaction {
            dao.putProducts(listOf(product.copy(physicalOverride=physical+delta,updatedAt=System.currentTimeMillis())))
            dao.putProductionOps(listOf(op.copy(qty=targetQty,status=if(delete)"Удален" else "Скорректирован")))
            dao.putProductionReceiptLines(listOf(line.copy(qty=targetQty,amountRub=targetQty*line.rateRub)))
            if(receipt!=null){
                val allLines=dao.productionReceiptLines(line.documentId)
                dao.putProductionReceipts(listOf(receipt.copy(totalQty=allLines.sumOf{it.qty},totalAmount=allLines.sumOf{it.amountRub})))
            }
            dao.putProductionAudit(
                listOf(
                    ProductionAuditEntity(
                        id="PA-"+java.util.UUID.randomUUID().toString().replace("-","").take(10).uppercase(),
                        documentId=line.documentId,opId=opId,action=if(delete)"DELETE" else "CORRECT",
                        oldValue=oldJson,newValue=JSONObject().put("qty",targetQty).put("status",if(delete)"Удален" else "Скорректирован").toString(),
                        userId=userId
                    )
                )
            )
        }
    }

    suspend fun presenceToday():List<PresenceSessionEntity> =
        dao.presenceSessions(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))

    suspend fun adminDailyStatus(date:String):AdminDailyStatusEntity? = dao.adminDailyStatus(date)

    suspend fun saveAdminDailyStatus(date:String,dayOff:Boolean,reason:String) {
        dao.putAdminDailyStatus(listOf(AdminDailyStatusEntity(date,dayOff,reason.trim())))
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
    val lines=JSONArray().apply {
        this@toEntity.lines.forEach { line ->
            put(JSONObject().apply {
                put("sku",line.sku)
                put("name",line.name)
                put("qty",line.qty)
                put("price",line.price)
                put("discountPct",line.discountPct)
            })
        }
    }
    val payload=JSONObject().apply {
        put("lines",lines)
        put("recipient",this@toEntity.recipient)
        put("contactPhone",this@toEntity.contactPhone)
        put("deliveryDate",this@toEntity.deliveryDate)
        put("deliveryTime",this@toEntity.deliveryTime)
    }
    val events=JSONArray().apply {
        this@toEntity.history.forEach { event ->
            put(JSONObject().apply {
                put("status",event.status)
                put("dateTime",event.dateTime)
                put("actor",event.actor)
            })
        }
    }
    return OrderEntity(id,clientName,dateTime,payload.toString(),status,events.toString(),deliveryMethod,deliveryAddress,comment)
}

private fun OrderEntity.toProto():ProtoOrder {
    val raw=linesJson.trim()
    val payload=if(raw.startsWith("[")) null else runCatching { JSONObject(raw) }.getOrNull()
    val lineArray=payload?.optJSONArray("lines") ?: JSONArray(raw)
    val lines=(0 until lineArray.length()).map { i ->
        lineArray.getJSONObject(i).let {
            ProtoOrderLine(
                sku=it.getString("sku"),
                name=it.getString("name"),
                qty=it.getInt("qty"),
                price=it.getInt("price"),
                discountPct=it.optInt("discountPct",0)
            )
        }
    }
    val eventArray=JSONArray(historyJson)
    val events=(0 until eventArray.length()).map { i ->
        eventArray.getJSONObject(i).let {
            ProtoOrderEvent(it.getString("status"),it.getString("dateTime"),it.getString("actor"))
        }
    }
    return ProtoOrder(
        id=id,
        clientName=clientName,
        dateTime=dateTime,
        lines=lines,
        status=status,
        history=events,
        deliveryMethod=deliveryMethod,
        deliveryAddress=deliveryAddress,
        comment=comment,
        recipient=payload?.optString("recipient").orEmpty(),
        contactPhone=payload?.optString("contactPhone").orEmpty(),
        deliveryDate=payload?.optString("deliveryDate").orEmpty(),
        deliveryTime=payload?.optString("deliveryTime").orEmpty()
    )
}

private fun ProtoProductionOp.toEntity(id:String)=ProductionOpEntity(id,date,time,sku,name,qty,assembler,postedBy,status)
private fun ProductionOpEntity.toProto(
    rateRub:Int=0,
    amountRub:Int=0,
    documentId:String=""
)=ProtoProductionOp(date,time,sku,name,qty,assembler,postedBy,status,rateRub,amountRub,documentId)


private fun ChatMessageEntity.toModel()=SansaraChatMessage(
    id=id,
    conversationId=conversationId,
    senderRole=senderRole,
    senderId=senderId,
    body=body,
    attachmentUri=attachmentUri,
    attachmentName=attachmentName,
    attachmentMime=attachmentMime,
    createdAt=createdAt,
    read=read
)


private fun AgentCustomerEntity.toModel()=SansaraAgentCustomer(
    id=id,ownerClientId=ownerClientId,fullName=fullName,phone=phone,email=email,city=city,address=address,comment=comment,createdAt=createdAt
)

private fun AgentSettingsEntity.toModel()=SansaraAgentSettings(
    ownerClientId=ownerClientId,generalMarkupEnabled=generalMarkupEnabled,generalMarkupPct=generalMarkupPct
)

private fun AgentCustomerMarkupEntity.toModel()=SansaraAgentMarkup(
    customerId=customerId,ownerClientId=ownerClientId,category=category,markupPct=markupPct
)

private fun AgentReminderEntity.toModel()=SansaraAgentReminder(
    id=id,ownerClientId=ownerClientId,customerId=customerId,customerName=customerName,remindAtEpochMs=remindAtEpochMs,note=note,active=active
)
