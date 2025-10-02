package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.adapter

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.retrofit.APICall

class AllUsersPagingSource(private val apiCall: APICall): PagingSource<Int, UserModel>() {

    override fun getRefreshKey(state: PagingState<Int, UserModel>): Int {
        return 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserModel> {
        try {

            val parameters = HashMap<String,Int>()

            if(params.key != null){
                parameters["page"] = params.key!!
            }
            else{
                parameters["page"] = 0
            }

            val response = apiCall.getAllUsers(parameters)

            if(response.code() == 200){
                val responseData = mutableListOf<UserModel>()

                val users = response.body()?.data?.users

                if(!users.isNullOrEmpty()){
                    for(item in users){
                        responseData.add(item)
                    }
                }

                val prevPage: Int?
                val nextPage: Int?

                if(responseData.isNullOrEmpty()){
                    if(params.key == null || params.key == 0){
                        prevPage = null
                        nextPage = null
                    }
                    else{
                        prevPage = params.key!! - 1
                        nextPage = null
                    }
                }
                else{
                    if(params.key == null || params.key == 0){
                        prevPage = null
                        nextPage =  1
                    }
                    else{
                        prevPage = params.key!! - 1
                        nextPage = params.key!! + 1
                    }
                }

                return LoadResult.Page(
                    data = responseData,
                    prevKey = prevPage,
                    nextKey = nextPage
                )
            }
            else{
                val e: Exception
                if(response.code() == 400){
                    e = Exception("Bad Request")
                    return LoadResult.Error(e)
                }
                else if(response.code() == 404){
                    e = Exception("Not Found")
                    return LoadResult.Error(e)
                }
                else if(response.code() == 500){
                    e = Exception("Internal Server Error")
                    return LoadResult.Error(e)
                }
                else{
                    e = Exception("Something went wrong, Please try later")
                    return LoadResult.Error(e)
                }
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}