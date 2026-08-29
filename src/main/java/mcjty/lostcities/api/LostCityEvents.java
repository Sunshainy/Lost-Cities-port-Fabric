package mcjty.lostcities.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.Consumer;

/**
 * Точки подписки на события генерации Lost Cities.
 * <p>
 * В оригинале на NeoForge события публиковались в общую шину
 * ({@code NeoForge.EVENT_BUS.post(...)}). На Fabric общей шины нет, поэтому каждое
 * событие получило свой {@link Event}. Подписка:
 * {@code LostCityEvents.PRE_GEN_CITY_CHUNK.register(event -> ...)}.
 * <p>
 * Отменяемые события ({@code PreGenCityChunkEvent}, {@code PreExplosionEvent})
 * отменяются вызовом {@link LostCityEvent#setCanceled(boolean)} внутри слушателя.
 */
public final class LostCityEvents {

    public static final Event<Consumer<LostCityEvent.CharacteristicsEvent>> CHARACTERISTICS = create();
    public static final Event<Consumer<LostCityEvent.PreGenCityChunkEvent>> PRE_GEN_CITY_CHUNK = create();
    public static final Event<Consumer<LostCityEvent.PostGenCityChunkEvent>> POST_GEN_CITY_CHUNK = create();
    public static final Event<Consumer<LostCityEvent.PostGenOutsideChunkEvent>> POST_GEN_OUTSIDE_CHUNK = create();
    public static final Event<Consumer<LostCityEvent.PreExplosionEvent>> PRE_EXPLOSION = create();

    private LostCityEvents() {
    }

    /** Разослать событие и вернуть его же, чтобы можно было прочитать флаг отмены. */
    public static <T extends LostCityEvent> T post(Event<Consumer<T>> hook, T event) {
        hook.invoker().accept(event);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static <T extends LostCityEvent> Event<Consumer<T>> create() {
        return EventFactory.createArrayBacked((Class<Consumer<T>>) (Class<?>) Consumer.class,
                callbacks -> event -> {
                    for (Consumer<T> callback : callbacks) {
                        callback.accept(event);
                    }
                });
    }
}
