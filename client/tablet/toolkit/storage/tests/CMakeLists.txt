add_executable(storage-test storage_test.cpp)
target_link_libraries(storage-test PRIVATE rm::storage)
add_test(NAME storage COMMAND storage-test)
