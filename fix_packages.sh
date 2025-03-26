#!/bin/bash
find app/src/main -name "*.kt" -exec sed -i "" "s/package com.newshifter.player/package com.songshifter/g" {} \;
find app/src/test -name "*.kt" -exec sed -i "" "s/package com.newshifter.player/package com.songshifter/g" {} \;
find app/src/main -name "*.kt" -exec sed -i "" "s/import com.newshifter.player\./import com.songshifter./g" {} \;
find app/src/test -name "*.kt" -exec sed -i "" "s/import com.newshifter.player\./import com.songshifter./g" {} \;
