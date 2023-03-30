package com.yeterkarakus.miniyoutube.view.searchpage.searchfragment.view


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yeterkarakus.miniyoutube.api.RetrofitApi
import com.yeterkarakus.miniyoutube.databinding.FragmentSearchBinding
import com.yeterkarakus.miniyoutube.model.common.ErrorResponse
import com.yeterkarakus.miniyoutube.model.searchmodel.SearchType
import com.yeterkarakus.miniyoutube.view.searchpage.searchfragment.model.AlbumViewModel
import com.yeterkarakus.miniyoutube.view.searchpage.searchfragment.model.SearchViewModel
import com.yeterkarakus.miniyoutube.view.searchpage.searchfragment.model.TrackViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

class SearchFragment @Inject constructor(
    private val retrofit : RetrofitApi
) : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchEditText.setOnEditorActionListener { v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                try {
                    binding.imageView.visibility = GONE
                    binding.lottieLoading.visibility = VISIBLE
                    getSearchData()
                }catch (e :Exception){
                    e.printStackTrace()
                }

            }
            false
        }

    }

    private fun getSearchData() {
        scope.launch {
            val searchTracksData = retrofit.search(
                binding.searchEditText.text.toString(),
                SearchType.tracks,
                limit = 10
            )



            val searchAlbumData = retrofit.search(
                binding.searchEditText.text.toString(),
                SearchType.albums,
                limit = 5
            )
            val trackList = mutableListOf<TrackViewModel>()
            val searchViewModel = SearchViewModel()
            searchViewModel.searchText = binding.searchEditText.text.toString()

            //Track
            searchTracksData.body()?.let {
                for (item in it.tracks.items) {
                    val track = TrackViewModel(
                        item.data.id,
                        item.data.name,
                        item.data.artists.items[0].profile.name,
                        item.data.albumOfTrack.coverArt.sources[0].url
                    )
                    trackList.add(track)
                }

                searchViewModel.trackRecordCount =it.tracks.totalCount
            }
            searchViewModel.trackList = trackList
            //Track

            //Album

            val albumList : MutableList<AlbumViewModel> = mutableListOf()
            searchAlbumData.body()?.let {

                for (item in it.albums.items) {

                    val album = AlbumViewModel(

                        item.data.uri.replace("spotify:album:",""),
                        item.data.name,
                        item.data.artists.items[0].profile.name,
                        item.data.coverArt.sources[0].url
                    )
                    albumList.add(album)
                }
                searchViewModel.albumRecordCount = it.albums.totalCount
            }
            searchViewModel.albumList = albumList


            withContext(Dispatchers.Main) {
                if (searchAlbumData.isSuccessful && searchTracksData.isSuccessful) {

                    val actionSearchActive = SearchFragmentDirections.actionSearchFragmentToSearchActiveFragment(
                            searchViewModel
                        )
                    findNavController().navigate(actionSearchActive)
                } else {
                    val gson = Gson()
                    val type = object : TypeToken<ErrorResponse>() {}.type
                    val errorResponse: ErrorResponse? = gson.fromJson(searchTracksData.errorBody()!!.charStream(), type)
                    if (errorResponse != null) {
                        Toast.makeText(requireContext(), errorResponse.message, Toast.LENGTH_LONG)
                            .show()

                        val actionError = SearchFragmentDirections.actionSearchFragmentToSearchNotFoundFragment()
                        findNavController().navigate(actionError)


                    }
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        job.cancel()
        super.onDestroy()
    }
}
