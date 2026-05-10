/**
 * Abstract base classes for widgets — intended to be extended, not instantiated.
 *
 * <p>This package holds widget abstractions that other widgets inherit from. Classes
 * here typically have abstract methods, protected helpers, or define shared lifecycle
 * contracts. They should not be referenced directly in layouts or by feature code.
 *
 * <h2>Current contents</h2>
 * <ul>
 *   <li>{@code BaseWidget} — common base for custom widgets</li>
 *   <li>{@code CollapsibleLayout} — base for collapsible layouts (extended by
 *       {@code CollapsibleEventLayout}, {@code CollapsibleComponentLayout})</li>
 *   <li>{@code CollapsibleViewHolder} — view holder pattern for collapsible items</li>
 * </ul>
 *
 * <h2>Boundary</h2>
 * <ul>
 *   <li>If a class is concrete and reusable as-is, place it in {@link pro.sketchware.widgets}</li>
 *   <li>If a class is internal to one editor feature, place it in {@link pro.sketchware.lib}</li>
 * </ul>
 */
package pro.sketchware.widgets.base;
