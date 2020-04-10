#!/bin/bash -e

pushd () {
    command pushd "$@" > /dev/null
}

popd () {
    command popd "$@" > /dev/null
}

LANGUAGES=("python" "ruby")
EXCLUDE=("*.src.zip" "bin" "docs" "doc" "logo")
rm -rf languages
mkdir languages
pushd languages
    for language in "${LANGUAGES[@]}"; do 
        if [ -d "$GRAALVM_HOME/languages/$language" ]; then
            cp -r "$GRAALVM_HOME/languages/$language" .
            echo "Copied $language"
            pushd $language
                for excluded in "${EXCLUDE[@]}"; do
                    rm -rf $excluded
                done
            popd
        else
            echo "Language $language is not installed!"
        fi
    done
    rm ../src/main/resources/filesystem.zip
    zip -rq ../src/main/resources/filesystem.zip .
    echo "Packaged language runtimes."
popd
rm -rf languages