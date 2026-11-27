#pragma once
#include "StrokeSurface.h"
#include <QtQml/qqml.h>

namespace Ink {
// Call once before loading QML. The host owns the display controller and router.
inline void registerTypes() { qmlRegisterType<StrokeSurface>("Ink.Toolkit", 1, 0, "StrokeSurface"); }
} // namespace Ink
