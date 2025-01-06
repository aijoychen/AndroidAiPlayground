package com.aijoy.aiplayground

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException

@Suppress("DEPRECATION")
@SuppressLint("QueryPermissionsNeeded")
class MainActivity : AppCompatActivity() {
    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recycler_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val item = intent.getParcelableExtra<Item>(KEY_ITEM)
        if (item != null) {
            setTitle(item.name)
            recyclerView.adapter = ItemListAdapter(item.getSortedChildren())
        } else {
            lifecycleScope.launch {
                loadAllActivities()
            }
        }
    }

    private suspend fun loadAllActivities() {
        val rootItems = Item("root")
        withContext(Dispatchers.Default) {
            val pm = packageManager
            val demoIntent = Intent("aijoy.intent.action.DEMO")
            demoIntent.setPackage(packageName)
            val resolveInfoList: List<ResolveInfo> =
                pm.queryIntentActivities(demoIntent, PackageManager.GET_META_DATA)
            resolveInfoList.forEach {
                val name = it.loadLabel(pm).toString()
                val intent = Intent().also { intent ->
                    intent.setClassName(
                        it.activityInfo.applicationInfo.packageName,
                        it.activityInfo.name
                    )
                }
                val fullPath = it.activityInfo.metaData.getString("path")
                if (!fullPath.isNullOrEmpty()) {
                    val splitPaths: List<String> = fullPath.split("/")
                    rootItems.put(splitPaths.toMutableList(), Item(name, intent))
                } else {
                    rootItems.put(Item(name, intent))
                }
            }
        }
        recyclerView.adapter = ItemListAdapter(rootItems.getSortedChildren())
    }

    data class Item(val name: String, val demoIntent: Intent? = null) : Parcelable {
        private val children: MutableMap<String, Item>? = if (demoIntent != null) {
            null
        } else {
            mutableMapOf()
        }

        val isFolders = demoIntent == null

        fun put(item: Item) {
            children?.put(item.name, item)
        }

        fun put(splitPaths: MutableList<String>, item: Item) {
            if (children == null) {
                throw InvalidParameterException("put item in an activity item")
            }
            if (splitPaths.isEmpty()) {
                put(item)
                return
            }
            val childName = splitPaths.removeAt(0)
            var child: Item? = children[childName]
            if (child == null) {
                child = Item(childName)
                children[childName] = child
            }
            child.put(splitPaths, item)
        }

        fun getSortedChildren(): List<Item> {
            return children!!.values.sortedWith { o1, o2 ->
                o1.name.compareTo(o2.name)
            }
        }

        fun showDetail(context: Context) {
            val intent = demoIntent
                ?: Intent(context, MainActivity::class.java).also {
                    it.putExtra(KEY_ITEM, this)
                }
            context.startActivity(intent)
        }

        constructor(parcel: Parcel) : this(
            name = parcel.readString() ?: "",
            demoIntent = if (parcel.readByte().toInt() == 1) {
                parcel.readParcelable<Intent>(Intent::class.java.classLoader)
            } else {
                null
            }
        ) {
            if (demoIntent == null) {
                val childrenSize = parcel.readInt()
                for (i in 0 until childrenSize) {
                    val key = parcel.readString() ?: continue
                    val value = parcel.readParcelable<Item>(Item::class.java.classLoader)
                    if (value != null) {
                        children?.put(key, value)
                    }
                }
            }
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeByte(if (demoIntent != null) 1 else 0)
            demoIntent?.let {
                parcel.writeParcelable(it, flags)
            }
            if (demoIntent == null) {
                parcel.writeInt(children?.size ?: 0)
                children?.forEach { (key, value) ->
                    parcel.writeString(key)
                    parcel.writeParcelable(value, flags)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Item> {
            override fun createFromParcel(parcel: Parcel): Item {
                return Item(parcel)
            }

            override fun newArray(size: Int): Array<Item?> {
                return arrayOfNulls(size)
            }
        }
    }

    internal class ItemListAdapter(private val items: List<Item>) :
        RecyclerView.Adapter<ItemListAdapter.ItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text1: TextView =
                itemView.findViewById(android.R.id.text1)

            @SuppressLint("UseCompatLoadingForDrawables")
            fun bind(item: Item) {
                text1.text = item.name
                text1.compoundDrawablePadding =
                    text1.context.resources.getDimensionPixelSize(R.dimen.dp_10)
                text1.setCompoundDrawablesWithIntrinsicBounds(
                    text1.context.getDrawable(if (item.isFolders) R.drawable.ic_folder else R.drawable.ic_ai),
                    null, null, null
                )
                itemView.setOnClickListener { v: View -> item.showDetail(v.context) }
            }
        }
    }

    companion object {
        const val KEY_ITEM = "item"
    }
}