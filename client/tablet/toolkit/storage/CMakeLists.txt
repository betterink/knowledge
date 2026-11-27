add_library(rm-storage STATIC Database.cpp Database.h)
add_library(rm::storage ALIAS rm-storage)
# Prefer static SQLite so the device's library version cannot matter.
find_library(SQLITE3_LIBRARY NAMES libsqlite3.a sqlite3 REQUIRED)
find_path(SQLITE3_INCLUDE_DIR sqlite3.h REQUIRED)
find_package(Threads REQUIRED)
target_include_directories(rm-storage PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} PRIVATE ${SQLITE3_INCLUDE_DIR})
target_link_libraries(rm-storage PUBLIC Qt6::Core PRIVATE ${SQLITE3_LIBRARY} Threads::Threads ${CMAKE_DL_LIBS})
