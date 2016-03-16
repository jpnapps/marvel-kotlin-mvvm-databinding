package com.dougritter.marvelmovies

import android.content.Context
import android.content.Intent
import android.databinding.*
import android.support.v7.widget.SearchView
import android.util.Log
import android.widget.ImageView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*

object ViewModel {
    class CharacterViewModel(val context: Context, var model: Model.Character) {

        companion object {
            val IMAGE_TYPE = "/landscape_incredible."
        }

        var imageUrl = modelImageUrl()

        fun modelImageUrl(): String = model.thumbnail.path + IMAGE_TYPE + model.thumbnail.extension

        object ImageViewBindingAdapter {
            @BindingAdapter("bind:imageUrl")
            @JvmStatic
            fun loadImage(view: ImageView, url: String) {
                Picasso.with(view.context).load(url).into(view)
            }
        }

        public fun openDetailActivity() {
            var intent = Intent(context, DetailActivity::class.java)
            val json = Gson().toJson(model)
            intent.putExtra(DetailActivity.MODEL_EXTRA, json)
            context.startActivity(intent)
        }
    }

    class MainViewModel(val context: Context) {

        lateinit var originalList: MutableList<Model.Character>
        val defaultLimit = 20
        var countLimit = 0
        lateinit var service: MarvelService

        init {
            service = MarvelService.create()
        }

        interface MainActivityViewModel { fun endCallProgress(any: Any?) }

        private var _compoSub = CompositeSubscription()
        private val compoSub: CompositeSubscription
            get() {
                if (_compoSub.isUnsubscribed) {
                    _compoSub = CompositeSubscription()
                }
                return _compoSub
            }

        protected final fun manageSub(s: Subscription) = compoSub.add(s)

        fun unsubscribe() { compoSub.unsubscribe() }

        fun filterList(term: String, adapter: CharactersAdapter) {
            if (term != "") {
                val list = adapter.characterResponse.data.results.filter { it.name.contains(term.trim(), true) } as MutableList<Model.Character>
                adapter.characterResponse.data.results = list
                adapter.notifyDataSetChanged()

            } else {
                adapter.characterResponse.data.results = originalList
                adapter.notifyDataSetChanged()
            }

        }

        fun getOnQueryTextChange(adapter: CharactersAdapter) : SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener{
            override fun onQueryTextChange(term: String?): Boolean {
                if (term != null) { filterList(term, adapter) }
                return false
            }
            override fun onQueryTextSubmit(term: String?): Boolean {
                if (term != null) { filterList(term, adapter) }
                return false
            }
        }


        fun loadCharactersList(callback: MainActivityViewModel) {

            val timestamp = Date().time;
            val hash = Utils.md5(timestamp.toString()+BuildConfig.MARVEL_PRIVATE_KEY+BuildConfig.MARVEL_PUBLIC_KEY)

            manageSub(
                    service.getCharacters(timestamp.toString(), BuildConfig.MARVEL_PUBLIC_KEY, hash, defaultLimit)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe( { c -> callback.endCallProgress(c)
                                originalList = c.data.results
                                countLimit = c.data.limit},
                                    { e -> callback.endCallProgress(e)
                                        Log.e(MainActivity::class.java.simpleName, e.message)})
            )
        }

        fun loadMoreCharacters(callback: MainActivityViewModel, adapter: CharactersAdapter) {
            val timestamp = Date().time;
            val hash = Utils.md5(timestamp.toString()+BuildConfig.MARVEL_PRIVATE_KEY+BuildConfig.MARVEL_PUBLIC_KEY)

            manageSub(
                    service.getCharacters(timestamp.toString(), BuildConfig.MARVEL_PUBLIC_KEY, hash, countLimit + defaultLimit)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe( { c -> callback.endCallProgress(c)
                            updateIndexesForRequests(adapter, c)},
                                    { e -> callback.endCallProgress(e)
                                        Log.e(MainActivity::class.java.simpleName, e.message)})
            )
        }

        public fun updateIndexesForRequests(adapter: CharactersAdapter, response: Model.CharacterResponse) {
            adapter.characterResponse = response
            adapter.notifyItemRangeChanged(countLimit, countLimit + defaultLimit)
            originalList = response.data.results
            countLimit += defaultLimit
        }

    }


    class CharacterDetailViewModel(val context: Context, var intent: Intent) {

        lateinit var service: MarvelService
        lateinit var model: Model.Character
        companion object { val IMAGE_TYPE = "/standard_fantastic." }

        init {
            service = MarvelService.create()
            val characterType = object : TypeToken<Model.Character>() {}.type
            model = Gson().fromJson<Model.Character>(intent.getStringExtra(DetailActivity.MODEL_EXTRA), characterType)
        }

        interface DetailViewModel { fun endCallProgress(any: Any?) }

        var detailImageUrl = detailImageUrl()

        fun detailImageUrl(): String = model.thumbnail.path + IMAGE_TYPE + model.thumbnail.extension

        object ImageViewBindingAdapter {
            @BindingAdapter("bind:detailImageUrl")
            @JvmStatic
            fun loadImage(view: ImageView, url: String) {
                Picasso.with(view.context).load(url).into(view)
            }
        }

        private var _compoSub = CompositeSubscription()
        private val compoSub: CompositeSubscription
            get() {
                if (_compoSub.isUnsubscribed) {
                    _compoSub = CompositeSubscription()
                }
                return _compoSub
            }

        protected final fun manageSub(s: Subscription) = compoSub.add(s)

        fun unsubscribe() { compoSub.unsubscribe() }

        fun loadCharacter(callback: DetailViewModel) {
            val timestamp = Date().time;
            val hash = Utils.md5(timestamp.toString()+BuildConfig.MARVEL_PRIVATE_KEY+BuildConfig.MARVEL_PUBLIC_KEY)

            manageSub(
                    service.getCharacterDetail(model.id.toString(), timestamp.toString(), BuildConfig.MARVEL_PUBLIC_KEY, hash)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe( { c -> callback.endCallProgress(c)},
                                    { e -> callback.endCallProgress(e)
                                        Log.e(DetailActivity::class.java.simpleName, e.message)})
            )

        }


    }

}