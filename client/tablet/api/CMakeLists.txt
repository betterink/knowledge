add_library(rm-api STATIC ApiModels.cpp ApiModels.h)
add_library(rm::api ALIAS rm-api)
target_include_directories(rm-api PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(rm-api PUBLIC Qt6::Core)
