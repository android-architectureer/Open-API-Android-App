package com.codingwithmitch.openapi.ui.main.blog

import android.util.Log
import androidx.lifecycle.*
import com.codingwithmitch.openapi.models.BlogPost
import com.codingwithmitch.openapi.repository.main.BlogRepository
import com.codingwithmitch.openapi.session.SessionManager
import com.codingwithmitch.openapi.ui.BaseViewModel
import com.codingwithmitch.openapi.ui.DataState
import com.codingwithmitch.openapi.ui.Loading
import com.codingwithmitch.openapi.ui.main.blog.state.BlogStateEvent
import com.codingwithmitch.openapi.ui.main.blog.state.BlogStateEvent.*
import com.codingwithmitch.openapi.ui.main.blog.state.BlogViewState
import com.codingwithmitch.openapi.util.AbsentLiveData
import javax.inject.Inject

class BlogViewModel
@Inject
constructor(
    private val sessionManager: SessionManager,
    private val blogRepository: BlogRepository
)
    : BaseViewModel<BlogStateEvent, BlogViewState>()
{

    init {
        // set empty list to start
        setBlogListData(ArrayList<BlogPost>())
    }
    override fun handleStateEvent(stateEvent: BlogStateEvent): LiveData<DataState<BlogViewState>> {
        when(stateEvent){
            is BlogSearchEvent -> {
                return sessionManager.cachedToken.value?.let { authToken ->
                    blogRepository.searchBlogPosts(
                        authToken,
                        stateEvent.searchQuery,
                        stateEvent.order, // can't be null. see @BlogViewState class
                        stateEvent.page // can't be null. see @BlogViewState class
                    )
                }?: AbsentLiveData.create()
            }

             is NextPageEvent -> {
                 Log.d(TAG, "BlogViewModel: NextPageEvent detected...")
                 return sessionManager.cachedToken.value?.let { authToken ->
                     blogRepository.searchBlogPosts(
                         authToken,
                         viewState.value!!.searchQuery,
                         viewState.value!!.order,
                         viewState.value!!.page
                     )
                 }?: AbsentLiveData.create()
             }


            // is ChangeOrderEvent
            //  -> New filter criteria is selected

            // is BlogSelectedEvent
            //  -> Select a blog post from the list. Navigate to ViewBlogFragment

            is None ->{
                return object: LiveData<DataState<BlogViewState>>(){
                    override fun onActive() {
                        super.onActive()
                        value = DataState(null, Loading(false), null)
                    }
                }
            }
        }
    }

    fun loadInitialBlogs(){
        // if the user hasn't made a query yet, show some blogs
        viewState.value?.let {
            if(it.blogList.size == 0){
                loadFirstPage("")
            }
        }
    }

    fun loadFirstPage(query: String) {
        setQueryInProgress(true)
        setQueryExhausted(false)
        resetPage()
        setQuery(query)
        setStateEvent(BlogSearchEvent(query))
        Log.e(TAG, "BlogViewModel: loadFirstPage: ${viewState.value?.page}")
    }

    fun loadNextPage(){
        if(!viewState.value!!.isQueryInProgress && !viewState.value!!.isQueryExhausted){
            Log.d(TAG, "BlogViewModel: Attempting to load next page...")
            setQueryInProgress(true)
            incrementPageNumber()
            setStateEvent(NextPageEvent())
        }
    }

    fun resetPage(){
        val update = getCurrentViewStateOrNew()
        update.page = 1
        _viewState.value = update
    }

    fun setQuery(query: String){
        val update = getCurrentViewStateOrNew()
        update.searchQuery = query
        _viewState.value = update
    }

    fun setBlogListData(blogList: List<BlogPost>){
        val update = getCurrentViewStateOrNew()
        if(update.blogList == blogList){
            return
        }
        update.blogList = blogList
        _viewState.value = update
    }

    fun incrementPageNumber(){
        val update = getCurrentViewStateOrNew()
        val page = update.copy().page
        update.page = page + 1
        _viewState.value = update
    }

    fun setQueryExhausted(isExhausted: Boolean){
        val update = getCurrentViewStateOrNew()
        update.isQueryExhausted = isExhausted
        _viewState.value = update
    }

    fun setQueryInProgress(isInProgress: Boolean){
        val update = getCurrentViewStateOrNew()
        update.isQueryInProgress = isInProgress
        _viewState.value = update
    }

    fun getCurrentViewStateOrNew(): BlogViewState{
        val value = viewState.value?.let{
            it
        }?: BlogViewState()
        return value
    }

    fun cancelRequests(){
        blogRepository.cancelRequests()
        handlePendingData()
    }

    fun handlePendingData(){
        setStateEvent(None())
    }

    override fun onCleared() {
        super.onCleared()
        cancelRequests()
    }

    
}

















