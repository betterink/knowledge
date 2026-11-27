add_executable(interaction-benchmark main.cpp application.qrc)
target_link_libraries(interaction-benchmark PRIVATE rm::interaction)
if(RM_TOOLKIT_BUILD_TESTS)
    add_test(NAME interaction-benchmark-smoke COMMAND interaction-benchmark --smoke-test)
    set_tests_properties(interaction-benchmark-smoke PROPERTIES
        ENVIRONMENT "QT_QPA_PLATFORM=offscreen;QT_QUICK_BACKEND=software")
endif()
install(TARGETS interaction-benchmark RUNTIME DESTINATION bin)
