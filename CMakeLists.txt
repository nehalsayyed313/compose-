cmake_minimum_required(VERSION 3.16)

project(MyQtApp VERSION 0.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Qt6 6.5 REQUIRED COMPONENTS Quick QuickControls2)

qt_standard_project_setup(REQUIRES 6.5)

qt_add_executable(appMyQtApp
    main.cpp
)

qt_add_qml_module(appMyQtApp
    URI MyQtApp
    VERSION 1.0
    QML_FILES
        Main.qml
)

set_target_properties(appMyQtApp PROPERTIES
    MACOSX_BUNDLE_GUI_IDENTIFIER com.example.MyQtApp
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
)

target_link_libraries(appMyQtApp PRIVATE
    Qt6::Quick
    Qt6::QuickControls2
)
