#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#define ID_BTN_TEST 101

LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_CREATE: {
            // Add a clickable button
            CreateWindowW(
                L"BUTTON", L"Click to Test Wine",
                WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
                50, 45, 180, 40,
                hwnd, (HMENU)ID_BTN_TEST, NULL, NULL
            );
            break;
        }
        case WM_COMMAND: {
            if (LOWORD(wParam) == ID_BTN_TEST) {
                MessageBoxW(
                    hwnd,
                    L"Success!\n\nNative win-arm64 executable running under Termux Wine!",
                    L"Wine ARM64 Working",
                    MB_OK | MB_ICONINFORMATION
                );
            }
            break;
        }
        case WM_DESTROY: {
            PostQuitMessage(0);
            break;
        }
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
    return 0;
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    const wchar_t CLASS_NAME[] = L"WineArm64Class";

    WNDCLASSW wc = {0};
    wc.lpfnWndProc   = WndProc;
    wc.hInstance     = hInstance;
    wc.lpszClassName = CLASS_NAME;
    wc.hCursor       = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);

    RegisterClassW(&wc);

    HWND hwnd = CreateWindowExW(
        0, CLASS_NAME, L"win-arm64 Wine Test",
        WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX,
        CW_USEDEFAULT, CW_USEDEFAULT, 300, 180,
        NULL, NULL, hInstance, NULL
    );

    if (!hwnd) return 0;

    ShowWindow(hwnd, nCmdShow);
    UpdateWindow(hwnd);

    MSG msg;
    while (GetMessageW(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    return (int)msg.wParam;
}
