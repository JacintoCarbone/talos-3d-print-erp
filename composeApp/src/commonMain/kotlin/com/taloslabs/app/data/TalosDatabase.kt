package com.taloslabs.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "filamentos")
data class FilamentoEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val marca: String,
    val color: String,
    val gramos: Double,
    val totalGramos: Double = 1000.0,
    val colorHex: String = "#6B7280"
)

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val tipo: String, // venta | gasto
    val fecha: String,
    val monto: Double,
    val comentario: String,
    val pedidoId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pedidos")
data class PedidoEntity(
    @PrimaryKey val id: String,
    val descripcion: String,
    val cliente: String,
    val telefono: String,
    val fecha: String,
    val estado: String = "pendiente", // pendiente | imprimiendo | listo | entregado
    val montoTotal: Double = 0.0
)

@Entity(
    tableName = "venta_filamentos",
    foreignKeys = [
        ForeignKey(
            entity = MovimientoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ventaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FilamentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["filamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ventaId"), Index("filamentoId")]
)
data class VentaFilamentoEntity(
    @PrimaryKey val id: String,
    val ventaId: String,
    val filamentoId: String,
    val gramos: Double,
    val costo: Double
)

@Entity(
    tableName = "pedido_filamentos",
    foreignKeys = [
        ForeignKey(
            entity = PedidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FilamentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["filamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pedidoId"), Index("filamentoId")]
)
data class PedidoFilamentoEntity(
    @PrimaryKey val id: String,
    val pedidoId: String,
    val filamentoId: String,
    val gramos: Double
)

@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val id: Int = 1,
    val costoPorHora: Double = 20.0,
    val costoPorKiloFilamento: Double = 600.0,
    val porcentajeGanancia: Double = 60.0,
    val porcentajePersonalizacion: Double = 10.0
)

@Dao
interface TalosDao {
    @Query("SELECT * FROM filamentos ORDER BY marca, tipo, color")
    fun filamentos(): Flow<List<FilamentoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFilamento(item: FilamentoEntity)

    @Delete
    suspend fun deleteFilamento(item: FilamentoEntity)

    @Query("SELECT * FROM movimientos ORDER BY createdAt DESC")
    fun movimientos(): Flow<List<MovimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovimiento(item: MovimientoEntity)

    @Delete
    suspend fun deleteMovimiento(item: MovimientoEntity)

    @Query("SELECT * FROM venta_filamentos WHERE ventaId = :ventaId")
    suspend fun usosDeVenta(ventaId: String): List<VentaFilamentoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUso(uso: VentaFilamentoEntity)

    @Query("DELETE FROM venta_filamentos WHERE ventaId = :ventaId")
    suspend fun deleteUsosDeVenta(ventaId: String)

    @Query("UPDATE filamentos SET gramos = gramos + :delta WHERE id = :filamentoId")
    suspend fun ajustarStock(filamentoId: String, delta: Double)

    @Query("DELETE FROM filamentos WHERE gramos <= 0")
    suspend fun eliminarFilamentosAgotados()

    @Query("SELECT * FROM config WHERE id = 1")
    fun config(): Flow<ConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: ConfigEntity)

    @Query("SELECT COUNT(*) FROM filamentos")
    suspend fun countFilamentos(): Int

    @Query("SELECT COUNT(*) FROM movimientos")
    suspend fun countMovimientos(): Int

    @Query("DELETE FROM filamentos")
    suspend fun clearFilamentos()

    @Query("DELETE FROM movimientos")
    suspend fun clearMovimientos()

    @Query("DELETE FROM venta_filamentos")
    suspend fun clearVentas()

    @Query("SELECT * FROM pedidos ORDER BY fecha DESC")
    fun pedidos(): Flow<List<PedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPedido(item: PedidoEntity)

    @Delete
    suspend fun deletePedido(item: PedidoEntity)

    @Query("UPDATE pedidos SET estado = :nuevoEstado WHERE id = :id")
    suspend fun actualizarEstadoPedido(id: String, nuevoEstado: String)

    @Query("UPDATE pedidos SET montoTotal = :monto WHERE id = :id")
    suspend fun actualizarMontoPedido(id: String, monto: Double)

    @Query("SELECT * FROM pedido_filamentos WHERE pedidoId = :pedidoId")
    suspend fun filamentosDePedido(pedidoId: String): List<PedidoFilamentoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedidoFilamento(item: PedidoFilamentoEntity)

    @Query("DELETE FROM pedido_filamentos WHERE pedidoId = :pedidoId")
    suspend fun deletePedidoFilamentos(pedidoId: String)

    @Query("SELECT * FROM movimientos WHERE pedidoId = :pedidoId LIMIT 1")
    suspend fun movimientoDePedido(pedidoId: String): MovimientoEntity?
}

@Database(
    entities = [
        FilamentoEntity::class,
        MovimientoEntity::class,
        VentaFilamentoEntity::class,
        ConfigEntity::class,
        PedidoEntity::class,
        PedidoFilamentoEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class TalosDatabase : RoomDatabase() {
    abstract fun dao(): TalosDao

    companion object {
        @Volatile private var INSTANCE: TalosDatabase? = null

        fun get(context: Context): TalosDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TalosDatabase::class.java,
                    "talos.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
