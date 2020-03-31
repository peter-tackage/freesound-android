/*
 * Copyright 2016 Futurice GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.futurice.freesound.feature.common.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.futurice.freesound.arch.mvvm.viewholder.BindingViewHolder
import com.futurice.freesound.feature.common.DisplayableItem

/**
 * Implementation of [RecyclerView.ListAdapter] for [DisplayableItem].
 */
class MultiItemListAdapter<I>(
        diffItemCallback: DiffUtil.ItemCallback<DisplayableItem<I>>, // FIXME This might need a map too or decorate one
        private val factoryMap: Map<Int, ViewHolderFactory>, // FIXME A ViewHolderFactoryFactory??
        private val binderMap: Map<Int, ViewHolderBinder<I>>) : // FIXME Could also be FactoryFactory, or a registry.
        ListAdapter<DisplayableItem<I>, RecyclerView.ViewHolder>(diffItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return factoryMap[viewType]?.createViewHolder(parent)
                ?: throw IllegalArgumentException("No ViewHolderFactory for viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item: DisplayableItem<I> = getItem(position)
        binderMap[item.type]?.bind(holder, item)
                ?: throw IllegalArgumentException("No ViewBinder for DisplayableItem type: ${item.type}")
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        // TODO Fix this - we should arguably limit the range of the ViewHolder rather than this check.
        (holder as? BindingViewHolder<*>)?.unbind()
    }

}