def enum(**enums):
    return type('Enum', (), enums)


OUTPUT_COLORS = enum(
    OK='\033[0;32m',
    WARN='\033[0;33m',
    ERROR='\033[0;31m',
    ENDC='\033[0;38m'
)


def error(content):
    print "{color_error}{content}{color_end}" \
        .format(color_error=OUTPUT_COLORS.ERROR, content=str(content), color_end=OUTPUT_COLORS.ENDC)


def warn(content):
    print "{color_error}{content}{color_end}" \
        .format(color_error=OUTPUT_COLORS.WARN, content=str(content), color_end=OUTPUT_COLORS.ENDC)


def ok(content):
    print "{color_error}{content}{color_end}" \
        .format(color_error=OUTPUT_COLORS.OK, content=str(content), color_end=OUTPUT_COLORS.ENDC)


def neutral(content):
    print content
