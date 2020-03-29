package com.futurice.freesound.common.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futurice.freesound.arch.mvvm.viewholder.BindingViewHolder
import com.futurice.freesound.feature.common.DisplayableItem
import com.futurice.freesound.feature.common.ui.adapter.MultiItemListAdapter
import com.futurice.freesound.feature.common.ui.adapter.ViewHolderBinder
import com.futurice.freesound.feature.common.ui.adapter.ViewHolderFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class MultiItemListAdapterTest {
    @Mock
    private lateinit var factory1: ViewHolderFactory

    @Mock
    private lateinit var factory2: ViewHolderFactory

    @Mock
    private lateinit var binder1: ViewHolderBinder<String>

    @Mock
    private lateinit var binder2: ViewHolderBinder<String>

    @Rule
    @JvmField
    var thrown = ExpectedException.none()

    private val diffCallback = object : DiffUtil.ItemCallback<DisplayableItem<String>>() {
        override fun areItemsTheSame(oldItem: DisplayableItem<String>, newItem: DisplayableItem<String>): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: DisplayableItem<String>, newItem: DisplayableItem<String>): Boolean {
            return true
        }
    }

    private lateinit var adapter: MultiItemListAdapter<String>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        adapter = MultiItemListAdapter(diffCallback, factoryMap(), binderMap())
    }

    @Test
    fun onCreateViewHolder_throws_IllegalArgumentException_whenTypeDoesNotExist() {
        // given
        thrown.expect(IllegalArgumentException::class.java)

        // when, throws
        adapter.onCreateViewHolder(mock(ViewGroup::class.java), 5)
    }

    @Test
    fun createViewHolder_creates_ViewHolder_from_associated_type_factory() {
        // given
        val viewGroup = mock(ViewGroup::class.java)
        `when`(factory1.createViewHolder(any()))
                .thenReturn(mock(RecyclerView.ViewHolder::class.java))

        // when
        adapter.onCreateViewHolder(viewGroup, 1)

        // then
        verify(factory1).createViewHolder(viewGroup)
    }

    @Test
    fun binder_for_associated_type_binds_item_to_ViewHolder() {
        // given
        val itemList = itemList()
        val viewHolder = mock(RecyclerView.ViewHolder::class.java)
        adapter.submitList(itemList)

        // when
        adapter.onBindViewHolder(viewHolder, 1)

        // then
        verify(binder2).bind(viewHolder, itemList[1])
    }

    @Test
    fun onViewRecycled_unbinds_item_from_ViewHolder_when_is_instance_of_BindingViewHolder() {
        // given
        val viewHolder = mock(BindingViewHolder::class.java)

        // when
        adapter.onViewRecycled(viewHolder)

        // then
        verify(viewHolder).unbind()
    }

    @Test
    fun onViewRecycled_doesNot_unbind_item_from_ViewHolder_when_is_not_BaseBindingViewHolder() {
        // given
        val viewHolder = mock(RecyclerView.ViewHolder::class.java)

        // when
        adapter.onViewRecycled(viewHolder)

        // then
        verifyZeroInteractions(viewHolder)
    }

    private fun factoryMap(): Map<Int, ViewHolderFactory> =
            hashMapOf(1 to factory1, 2 to factory2)

    private fun binderMap(): Map<Int, ViewHolderBinder<String>> =
            hashMapOf(1 to binder1, 2 to binder2)

    private fun itemList() = listOf(
            DisplayableItem("a", 1),
            DisplayableItem("b", 2),
            DisplayableItem("c", 1))

}