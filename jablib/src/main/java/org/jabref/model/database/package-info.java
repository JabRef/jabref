/**
 * Core data model types for JabRef databases.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Represent bibliographic databases and their metadata (mode, sorting, groups, etc.).</li>
 *   <li>Provide access to entries and collections in a way that is independent of the GUI.</li>
 *   <li>Offer primitives used by higher layers (logic/GUI) to manage and persist libraries.</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>This package is part of the domain model and must stay free of JavaFX or UI concerns.</li>
 *   <li>Favor immutability and clear ownership of state where feasible.</li>
 *   <li>Keep I/O and side effects in higher layers; focus on data and relations here.</li>
 * </ul>
 */
package org.jabref.model.database;
