# 🦾 Talos — 3D Printing ERP & Additive Manufacturing Manager

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room SQLite](https://img.shields.io/badge/Room_Database-SQLite-10B981?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)

**Talos** es una aplicación móvil nativa tipo **ERP (Enterprise Resource Planning)** diseñada para administrar integralmente talleres y negocios de **impresión 3D y manufactura aditiva** desde el celular.

Desarrollada con **Kotlin**, **Jetpack Compose** y persistencia relacional **Room (SQLite)** con *Kotlin Coroutines & Flow*.

---

## 🌟 Funcionalidades Principales

### 1. 📦 Control de Inventario de Filamentos en Tiempo Real
- Seguimiento exacto por bobina: **Marca, Material (PLA, PETG, ABS, TPU), Color (con selector cromático Hex)** y peso restante en gramos (`gramos` vs `totalGramos`).
- Descuento automático de gramos de filamento al registrar órdenes de producción y ventas.

### 2. 🏭 Gestión de Producción y Pedidos (`Kanban / Estados`)
- Flujo de estados de planta: `Pendiente` ➔ `Imprimiendo` ➔ `Listo` ➔ `Entregado`.
- Vinculación relacional (`ForeignKey` en cascada) entre pedidos de clientes y múltiples bobinas de filamento consumidas.
- Integración directa con contacto del cliente.

### 3. 🧮 Cotizador Paramétrico de Impresión 3D
- Motor de cálculo de costos basado en parámetros operativos configurables (`ConfigEntity`):
  - Costo energético/operativo por hora de máquina (`costoPorHora`).
  - Costo por kilogramo de filamento (`costoPorKiloFilamento`).
  - Margen de ganancia objetivo (`porcentajeGanancia`).
  - Recargo por diseño/personalización CAD (`porcentajePersonalizacion`).

### 4. 📊 Libro Mayor Financiero (`Ventas & Gastos`)
- Registro histórico de flujo de caja (`MovimientoEntity`), cálculo de costos reales de materia prima por venta (`VentaFilamentoEntity`) y métricas de rentabilidad.

---

## 🏗️ Arquitectura Técnica

```text
Talos/
├── composeApp/src/
│   ├── androidMain/
│   │   ├── AndroidManifest.xml
│   │   └── res/                        # Recursos gráficos, iconos adaptativos y temas
│   └── commonMain/kotlin/com/taloslabs/app/
│       ├── App.kt                      # UI declarativa en Jetpack Compose, gráficos Canvas y lógica de negocio
│       └── data/
│           └── TalosDatabase.kt        # Esquema relacional Room (6 entidades, claves foráneas, índices y DAOs reactivos con Flow)
├── build.gradle.kts
└── settings.gradle.kts
```

### Modelo de Datos Relacional (`Room SQLite`)
- **`filamentos` (`FilamentoEntity`)**: Inventario físico de materia prima.
- **`pedidos` (`PedidoEntity`)** & **`pedido_filamentos` (`PedidoFilamentoEntity`)**: Órdenes de trabajo y reserva de material.
- **`movimientos` (`MovimientoEntity`)** & **`venta_filamentos` (`VentaFilamentoEntity`)**: Contabilidad operativa y trazabilidad de consumo.
- **`config` (`ConfigEntity`)**: Parámetros técnicos y financieros de la granja de impresión.

---

## 🚀 Compilación y Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/JacintoCarbone/talos-3d-print-erp.git
   ```
2. Abrir el proyecto en **Android Studio**.
3. Ejecutar la configuración `composeApp` en un dispositivo físico Android o emulador:
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```

---

## 👨‍💻 Autor
**Jacinto Carbone** — *Tecnología en Mecatrónica (UTEC, Uruguay)*  
🌐 [https://jacintocarbone.github.io](https://jacintocarbone.github.io)
