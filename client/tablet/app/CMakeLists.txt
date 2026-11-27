add_executable(rm-knowledge main.cpp WorkspaceStore.cpp WorkspaceStore.h application.qrc)
target_link_libraries(rm-knowledge PRIVATE rm::toolkit)
add_test(NAME app-smoke COMMAND rm-knowledge --smoke-test)
set_tests_properties(app-smoke PROPERTIES
    ENVIRONMENT "QT_QPA_PLATFORM=offscreen;QT_QUICK_BACKEND=software")
install(TARGETS rm-knowledge RUNTIME DESTINATION bin)
