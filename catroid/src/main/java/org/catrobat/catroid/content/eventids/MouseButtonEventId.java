package org.catrobat.catroid.content.eventids;

public class MouseButtonEventId extends EventId {
    public final int buttonCode;

    public MouseButtonEventId(int buttonCode) {
        super(EventId.MOUSE_BUTTON_CLICKED);
        this.buttonCode = buttonCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MouseButtonEventId that = (MouseButtonEventId) o;
        return buttonCode == that.buttonCode;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + buttonCode;
    }
}