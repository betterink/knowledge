add_library(rm-network STATIC HttpClient.cpp HttpClient.h)
add_library(rm::network ALIAS rm-network)
target_include_directories(rm-network PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(rm-network PUBLIC Qt6::Core Qt6::Network)
