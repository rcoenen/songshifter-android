#!/bin/bash
find app/src/main -name "*.kt" -exec sed -i "" "s/package com.songshifter/package com.songshifter.app/g" {} \;
find app/src/test -name "*.kt" -exec sed -i "" "s/package com.songshifter/package com.songshifter.app/g" {} \;
find app/src/main -name "*.kt" -exec sed -i "" "s/import com.songshifter\./import com.songshifter.app./g" {} \;
find app/src/test -name "*.kt" -exec sed -i "" "s/import com.songshifter\./import com.songshifter.app./g" {} \;
