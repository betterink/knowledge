foreach(name canvas navigation display drag)
    add_executable(${name}-test ${name}_test.cpp)
    target_link_libraries(${name}-test PRIVATE rm::interaction)
    add_test(NAME ${name} COMMAND ${name}-test)
    set_tests_properties(${name} PROPERTIES
        ENVIRONMENT "QT_QPA_PLATFORM=offscreen;QT_QUICK_BACKEND=software")
endforeach()
target_sources(display-test PRIVATE DisplayTestBackend.h)
