package com.example.dbsyoki

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.graphics.Color.*
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit


class MainActivity : AppCompatActivity() {


    private lateinit var dbHelper: ProductDatabaseHelper
    private lateinit var chart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // SQLiteデータベースのヘルパーを初期化
        dbHelper = ProductDatabaseHelper(this)

        // BarChartの初期化
        chart = findViewById(R.id.barchart)

        // データベースからすべての商品を取得してグラフに表示
        val products = dbHelper.getAllProducts()

        // データをグラフに変換して表示
        setUpBarChart(products)
    }

    private fun setUpBarChart(products: List<Product>) {
        val entries = ArrayList<BarEntry>()

        // データをEntryに変換（消費期限までの日数をY値として使用）
        products.forEachIndexed { index, product ->
            val daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), product.expirationDate).toFloat()
            entries.add(BarEntry(index.toFloat(), daysUntilExpiration))
        }

        // BarDataSetの作成
        val dataSet = BarDataSet(entries, "消費期限までの日数")
        dataSet.color = BLUE

        // BarDataの作成
        val barData = BarData(dataSet)
        barData.barWidth = 0.9f // 棒の幅の調整

        // グラフの設定
        chart.data = barData
        chart.setFitBars(true) // 棒が完全に表示されるようにする
        chart.description.isEnabled = false // グラフの説明を非表示
        chart.setDrawGridBackground(false) // グリッド背景を非表示
        chart.animateY(1000) // グラフのアニメーション
        chart.invalidate() // グラフの更新
    }
}

class ProductDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "products.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_PRODUCTS = "products"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_EXPIRATION_DATE = "expirationDate"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // テーブルの作成
        val createTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_EXPIRATION_DATE TEXT
            )
        """
        db.execSQL(createTable)

        // 初期データの挿入
        insertInitialData(db)
    }

    // 初期データを挿入するメソッド
    private fun insertInitialData(db: SQLiteDatabase) {
        val initialProducts = listOf(
            Pair("食料", LocalDate.of(2024, 9,30 )),
            Pair("衣服", LocalDate.of(2024, 10, 15)),
            Pair("食料", LocalDate.of(2024, 8, 20))
        )

        for (product in initialProducts) {
            val values = ContentValues().apply {
                put(COLUMN_NAME, product.first)
                put(COLUMN_EXPIRATION_DATE, product.second.toString())
            }
            db.insert(TABLE_PRODUCTS, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    // すべての商品の取得メソッド
    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_EXPIRATION_DATE),
            null, null, null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                val expirationDate = LocalDate.parse(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_DATE)))
                products.add(Product(id, name, expirationDate))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return products
    }
}

// 商品のデータクラス
data class Product(val id: Int, val name: String, val expirationDate: LocalDate)