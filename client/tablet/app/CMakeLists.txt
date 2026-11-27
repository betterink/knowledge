add_executable(rm-knowledge main.cpp)
qt_policy(SET QTP0001 NEW)
qt_policy(SET QTP0004 NEW)
set_source_files_properties(ui/Theme.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_qml_module(rm-knowledge
    URI RmKnowledge
    SOURCES workspace/stores/WorkspaceStore.cpp workspace/stores/WorkspaceStore.h
    QML_FILES
        ui/Main.qml ui/Theme.qml ui/AppHeader.qml ui/Sidebar.qml ui/ActionMenu.qml
        ui/AppDialog.qml ui/Toast.qml ui/components/AppButton.qml
        workspace/screens/WorkspaceScreen.qml)
target_include_directories(rm-knowledge PRIVATE workspace/stores)
target_link_libraries(rm-knowledge PRIVATE rm::toolkit rm::api Qt6::QuickControls2)
add_test(NAME app-smoke COMMAND rm-knowledge --smoke-test)
set_tests_properties(app-smoke PROPERTIES
    ENVIRONMENT "QT_QPA_PLATFORM=offscreen;QT_QUICK_BACKEND=software")
install(TARGETS rm-knowledge RUNTIME DESTINATION bin)
add_test(NAME app-qmllint COMMAND ${CMAKE_COMMAND} --build ${CMAKE_BINARY_DIR} --target rm-knowledge_qmllint)
set_tests_properties(app-qmllint PROPERTIES FAIL_REGULAR_EXPRESSION "Warning: ")
