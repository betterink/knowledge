add_executable(network-test network_test.cpp)
target_link_libraries(network-test PRIVATE rm::network)
add_test(NAME network COMMAND network-test)
