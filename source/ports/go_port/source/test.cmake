# Sanitize the variables
string(REPLACE "\"" "" GOPATH "${GOPATH}")
string(REPLACE "\"" "" CGO_CFLAGS "${CGO_CFLAGS}")
string(REPLACE "\"" "" CGO_LDFLAGS "${CGO_LDFLAGS}")
string(REPLACE "\"" "" CMAKE_Go_COMPILER "${CMAKE_Go_COMPILER}")
string(REPLACE "\"" "" LIBRARY_PATH_NAME "${LIBRARY_PATH_NAME}")
string(REPLACE "\"" "" LIBRARY_PATH "${LIBRARY_PATH}")
string(REPLACE "\"" "" SANITIZER_COMPILE_DEFINITIONS "${SANITIZER_COMPILE_DEFINITIONS}")

if(NOT "${SANITIZER_COMPILE_DEFINITIONS}" STREQUAL "")
    set(CGO_CFLAGS "${CGO_CFLAGS} -D${SANITIZER_COMPILE_DEFINITIONS}")
    # TODO: Add sanitizer flags (-fsanitize=address...) to CGO_CFLAGS and CGO_LDFLAGS
endif()

# Override CGO flags
file(READ "${CMAKE_CURRENT_LIST_DIR}/go_port.go" FILE_CONTENTS)
string(REPLACE "#cgo CFLAGS: -Wall" "#cgo CFLAGS: ${CGO_CFLAGS}" FILE_CONTENTS "${FILE_CONTENTS}")
string(REPLACE "#cgo LDFLAGS: -lmetacall" "#cgo LDFLAGS: ${CGO_LDFLAGS}" FILE_CONTENTS "${FILE_CONTENTS}")
file(WRITE "${CMAKE_CURRENT_LIST_DIR}/go_port.go" "${FILE_CONTENTS}")

execute_process(COMMAND
	${CMAKE_COMMAND} -E env GOPATH=${GOPATH} ${LIBRARY_PATH_NAME}=${LIBRARY_PATH} ${CMAKE_Go_COMPILER} test
    WORKING_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}"
	RESULT_VARIABLE RESULT
    OUTPUT_VARIABLE OUTPUT
    ERROR_VARIABLE ERROR
)

# Restore CGO flags
string(REPLACE "#cgo CFLAGS: ${CGO_CFLAGS}" "#cgo CFLAGS: -Wall" FILE_CONTENTS "${FILE_CONTENTS}")
string(REPLACE "#cgo LDFLAGS: ${CGO_LDFLAGS}" "#cgo LDFLAGS: -lmetacall" FILE_CONTENTS "${FILE_CONTENTS}")
file(WRITE "${CMAKE_CURRENT_LIST_DIR}/go_port.go" "${FILE_CONTENTS}")

message(STATUS "${OUTPUT}")

if(RESULT)
	message(FATAL_ERROR "Go port failed with result: ${RESULT}\n${ERROR}")
endif()
