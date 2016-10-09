def enum(**enums):
    return type('Enum', (), enums)


OUTPUT_COLORS = enum(
    OK='\033[0;32m',
    WARN='\033[0;33m',
    ERROR='\033[0;31m',
    ENDC='\033[0;38m'
)


def error(content):
    print u"{color_error}{content}{color_end}".encode('utf8') \
        .format(color_error=OUTPUT_COLORS.ERROR, content=str(content.encode('utf8')), color_end=OUTPUT_COLORS.ENDC)


def warn(content):
    print u"{color_error}{content}{color_end}".encode('utf8') \
        .format(color_error=OUTPUT_COLORS.WARN, content=str(content.encode('utf8')), color_end=OUTPUT_COLORS.ENDC).encode('utf8')


def ok(content):
    print u"{color_error}{content}{color_end}".encode('utf8') \
        .format(color_error=OUTPUT_COLORS.OK, content=str(content.encode('utf8')), color_end=OUTPUT_COLORS.ENDC).encode('utf8')


def neutral(content):
    print content.encode('utf8')
