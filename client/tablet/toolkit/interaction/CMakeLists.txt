add_library(rm-interaction STATIC
    StrokeSurface.cpp StrokeSurface.h Toolkit.h
    TabletNavigation.cpp TabletNavigation.h
    EpaperDisplayControl.cpp EpaperDisplayControl.h)
add_library(rm::interaction ALIAS rm-interaction)
target_include_directories(rm-interaction PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
# Compiled into each consumer: a static library would drop the resource.
target_sources(rm-interaction INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/components.qrc)
target_link_libraries(rm-interaction PUBLIC Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick)
