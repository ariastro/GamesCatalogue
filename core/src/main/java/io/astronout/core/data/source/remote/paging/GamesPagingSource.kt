package io.astronout.core.data.source.remote.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.astronout.core.data.source.GamesStoryDataStore.Companion.NETWORK_PAGE_SIZE
import io.astronout.core.data.source.remote.web.ApiClient
import io.astronout.core.domain.model.Game
import retrofit2.HttpException
import java.io.IOException

class GamesPagingSource(
    private val apiClient: ApiClient
) : PagingSource<Int, Game>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Game> {
        val position = params.key ?: STARTING_PAGE_INDEX
        return try {
            val response = apiClient.getGames(position, params.loadSize)
            val repos = response.results
            val nextKey = if (repos.isNullOrEmpty()) {
                null
            } else {
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = repos?.map { Game(it) }.orEmpty(),
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Game>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    companion object {
        private const val STARTING_PAGE_INDEX = 1
    }

}