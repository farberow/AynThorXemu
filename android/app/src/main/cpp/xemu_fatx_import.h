#ifndef XEMU_FATX_IMPORT_H
#define XEMU_FATX_IMPORT_H

#include "block/block-common.h"
#include "qapi/error.h"

#include <stdbool.h>

bool xemu_fatx_import_dashboard(BlockBackend *blk,
                                const char *source_root,
                                const char *backup_root,
                                Error **errp);

#endif
