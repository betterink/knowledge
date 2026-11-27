add_executable(api-test api_test.cpp)
target_link_libraries(api-test PRIVATE rm::api)
target_compile_definitions(api-test PRIVATE FIXTURES="${CMAKE_CURRENT_SOURCE_DIR}")
add_test(NAME api COMMAND api-test)
