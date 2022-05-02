/**
 * Makes an element draggable via mouse cursorr.
 * Adapted from https://www.w3schools.com/howto/howto_js_draggable.asp
 */
class Draggable extends EventTarget {

    /**
     * @param draggableElement The element to be moved.
     * @param handleElement The element to serve as a handle for dragging.
     *      If null the draggableElement will be used as handle.
     */
    constructor(draggableElement, handleElement = null) {
        super();

        this.draggableElement = draggableElement;
        this.handleElement = handleElement ?? draggableElement;

        this._lastX = 0;
        this._lastY = 0;

        this._onMouseDown = this._onMouseDown.bind(this);
        this._onMouseUp = this._onMouseUp.bind(this);
        this._onMouseMove = this._onMouseMove.bind(this);
        this.handleElement.addEventListener('mousedown', this._onMouseDown);
    }

    _onMouseDown (event) {
        event.preventDefault();
        this._lastX = event.clientX;
        this._lastY = event.clientY;
        document.addEventListener('mouseup', this._onMouseUp);
        document.addEventListener('mousemove', this._onMouseMove);
        this.dispatchEvent(new CustomEvent('start'));
    }

    _onMouseMove (event) {
        event.preventDefault();
        const deltaX = event.clientX - this._lastX;
        const deltaY = event.clientY - this._lastY;
        this._lastX = event.clientX;
        this._lastY = event.clientY;
        this.draggableElement.style.top = `${this.draggableElement.offsetTop + deltaY}px`;
        this.draggableElement.style.left = `${this.draggableElement.offsetLeft + deltaX}px`;
        this.draggableElement.style.bottom = null;
        this.draggableElement.style.right = null;
        this.dispatchEvent(new CustomEvent('move'));
    }

    _onMouseUp (event) {
        document.removeEventListener('mouseup', this._onMouseUp);
        document.removeEventListener('mousemove', this._onMouseMove);
        this.dispatchEvent(new CustomEvent('stop'));
    }
}


export default Draggable;
