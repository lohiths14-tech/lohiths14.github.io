package com.smartfind.app.di

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class SmartFindGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // You can customize Glide's configuration here.
        // For example, set a memory cache size.
        val memoryCacheSizeBytes = 1024 * 1024 * 20 // 20mb
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))
    }
}
