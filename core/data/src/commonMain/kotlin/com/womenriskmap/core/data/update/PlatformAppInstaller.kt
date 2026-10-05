package com.womenriskmap.core.data.update

import com.womenriskmap.core.data.platform.PlatformContext

/** Android: FileProvider + system package installer. iOS and web: unsupported (stores/browser update the app). */
expect class PlatformAppInstaller(context: PlatformContext) : AppInstaller
