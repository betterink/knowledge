add_library(rm-toolkit STATIC
    core/StrokeSurface.cpp core/StrokeSurface.h core/Toolkit.h
    core/TabletNavigation.cpp core/TabletNavigation.h
    core/EpaperDisplayControl.cpp core/EpaperDisplayControl.h)
add_library(rm::toolkit ALIAS rm-toolkit)
target_include_directories(rm-toolkit PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/core)
target_link_libraries(rm-toolkit PUBLIC Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick)
