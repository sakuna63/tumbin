package com.sakuna63.tumbin.application.contract.presenter

import com.sakuna63.tumbin.application.contract.PostsContract
import com.sakuna63.tumbin.application.di.scope.ActivityScope
import com.sakuna63.tumbin.data.dao.DashboardRealmDao
import com.sakuna63.tumbin.data.dao.RealmResultsWrapper
import com.sakuna63.tumbin.data.datasource.PostDataSource
import com.sakuna63.tumbin.data.model.Post
import com.sakuna63.tumbin.extension.log
import com.trello.rxlifecycle.LifecycleProvider
import com.trello.rxlifecycle.android.ActivityEvent
import io.realm.RealmResults
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

@ActivityScope
class DashboardPresenter
@Inject
constructor(private val view: PostsContract.View,
            private val dataSource: PostDataSource,
            private val dashboardRealmDao: DashboardRealmDao,
            private val lifecycleProvider: LifecycleProvider<ActivityEvent>) : PostsContract.Presenter {

    private val realmResultsWrapper: RealmResultsWrapper<RealmResults<Post>> by lazy {
        dashboardRealmDao.findByTypes(Post.TYPE_PHOTO, Post.TYPE_TEXT, Post.TYPE_VIDEO)
    }
    private var isLoading = false
    private var hasMorePost = true

    @Suppress("unused")
    @Inject
    internal fun setupView() {
        this.view.setPresenter(this)
    }

    override fun init() {
        if (posts.isEmpty()) {
            showLoading()
            loadingPost(0, null, null, false)
            return
        }

        showPosts(posts)
    }

    override fun destroy() {
        realmResultsWrapper.close()
    }

    private fun loadingPost(offset: Int?, sinceId: Long?,
                            beforeId: Long?, forceRefreshCache: Boolean) {
        isLoading = true

        var single = dataSource.fetchDashboard(MAX_LIMIT, offset, sinceId, beforeId)
        if (forceRefreshCache) {
            single = dataSource.deleteAllDashboardCache().toSingleDefault(0)
                    .concatWith(single)
                    .last().toSingle()
        }

        single
                .compose(lifecycleProvider.bindToLifecycle<Int>().forSingle<Int>())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    isLoading = false
                    handleDashboardCallback(sinceId, it as Int)
                }, {
                    isLoading = false
                    it.log()
                    showError(it)
                })
    }

    private fun handleDashboardCallback(sinceId: Long?, numOfPosts: Int) {
        if (sinceId != null) { // refresh request
            showPosts(posts)
            // TODO: 2016/08/20 buggy
            // show glimpse of newer posts
            if (view.scrollY == 0) {
                view.scrollTo(sinceId, REFRESHING_OFFSET_PX)
            }
            return
        }

        hasMorePost = numOfPosts == MAX_LIMIT

        if (numOfPosts == 0 && posts.isEmpty()) {
            showEmpty()
        } else {
            showPosts(posts)
        }
    }

    override fun onPostClick(post: Post) {
        // TODO: make optional behavior
//        if (post.type == Post.TYPE_VIDEO && PostUtils.isExternalSource(post.videoType!!)) {
//            view.openBrowser(post.permalinkUrl!!)
//            return
//        }
        view.showPostDetail(post)
    }

    override fun onReloadClick() {
        showLoading()
        loadingPost(0, null, null, true)
    }

    override fun onRefresh() {
        showLoading()
        val latestId = posts[0].id
        loadingPost(null, latestId, null, false)
    }

    override fun onScrollBottom() {
        if (isLoading || !hasMorePost) {
            return
        }
        showLoading()
        val oldestId = posts[posts.size - 1].id
        loadingPost(null, null, oldestId, false)
    }

    private fun showPosts(posts: RealmResults<Post>) {
        view.hideLoading()
        view.showPosts(posts)
    }

    private fun showLoading() {
        view.hideEmpty()
        view.hideError()
        view.showLoading()
    }

    private fun showEmpty() {
        view.hideLoading()
        view.showEmpty()
    }

    private fun showError(e: Throwable) {
        view.hideLoading()
        view.showError(e.message!!)
    }

    private val posts: RealmResults<Post>
        get() {
            return realmResultsWrapper.results
        }

    companion object {
        private val MAX_LIMIT = 20
        private val REFRESHING_OFFSET_PX = -100
    }
}
