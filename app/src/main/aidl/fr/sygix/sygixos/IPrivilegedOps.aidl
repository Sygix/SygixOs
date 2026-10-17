/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos;

interface IPrivilegedOps {
    String run(String command) = 1;
    void destroy() = 16777114;
}
