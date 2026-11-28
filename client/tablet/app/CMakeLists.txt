add_executable(knowledge main.cpp application.qrc)
target_link_libraries(knowledge PRIVATE rm::toolkit)
add_test(NAME app-smoke COMMAND knowledge --smoke-test)
set_tests_properties(app-smoke PROPERTIES
    ENVIRONMENT "QT_QPA_PLATFORM=offscreen;QT_QUICK_BACKEND=software")
install(TARGETS knowledge RUNTIME DESTINATION bin)
