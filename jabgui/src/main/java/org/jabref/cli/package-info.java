/**
 * Command-line interface (CLI) utilities for JabRef.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Parse CLI arguments and map them to actions (import, export, convert, pseudonymize, etc.).</li>
 *   <li>Provide entry points that run without the GUI.</li>
 *   <li>Coordinate with services from {@code org.jabref.logic} to execute commands.</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Classes here should remain thin facades over core logic; avoid duplicating business rules.</li>
 *   <li>Prefer small, testable units and explicit argument objects over ad-hoc parsing.</li>
 * </ul>
 */
package org.jabref.cli;
