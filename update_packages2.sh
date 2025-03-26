#!/bin/bash
find app/src/main -name "*.kt" -exec sed -i "" "s/package com.songshifter.app/package com.newshifter.player/g" {} \;
find app/src/test -name "*.kt" -exec sed -i "" "s/package com.songshifter.app/package com.newshifter.player/g" {} \;
find app/src/main -name "*.kt" -exec sed -i "" "s/import com.songshifter.app\./import com.newshifter.player./g" {} \;
find app/src/test -name "*.kt" -exec sed -i "" "s/import com.songshifter.app\./import com.newshifter.player./g" {} \;
find app/src/main -name "*.kt" -exec sed -i "" "s/package com.songshifter/package com.newshifter.player/g" {} \;
find app/src/main -name "*.kt" -exec sed -i "" "s/import com.songshifter\./import com.newshifter.player./g" {} \;
