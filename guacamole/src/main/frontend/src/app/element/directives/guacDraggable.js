angular.module('element').directive('guacDraggable', ['$document', '$window', function($document, $window) {
  return {
      restrict: 'A',
      scope: {
        dragging: "="
      },
      link: function($scope, $element) {
          let shiftX = 0, shiftY = 0;
          let isDragging = false;

          const positionElement = function(yPosition, xPosition) {
            // position, making sure it's visible on the screen
            let top = yPosition > window.innerHeight - 40 ? window.innerHeight - 40 : yPosition;
            let left = xPosition > window.innerWidth - 40 ? window.innerWidth - 40 : xPosition;
            if (top < 0) {
              top = 0;
            }
            if (left < 0) {
              left = 0;
            }
            $element.css({
              top: top + 'px',
              left: left + 'px'
            });
          }

          const positionToStoredState = function() {
            const storedPosition = JSON.parse(localStorage.getItem('floatingButtonPosition')) || { top: 40, left: 40 };
            positionElement(storedPosition.top, storedPosition.left);
          }

          positionToStoredState();

          const onMouseDown = function(e) {
            $element.css('cursor', 'grabbing');
            shiftX = e.clientX - $element[0].getBoundingClientRect().left;
            shiftY = e.clientY - $element[0].getBoundingClientRect().top;
            $document.on('pointermove', onMouseMove);
            isDragging = true;
            $element[0].setPointerCapture(e.pointerId);
            requestAnimationFrame(updatePosition);
          }

          const onMouseMove = function(e) {
            positionElement(e.pageY - shiftY, e.pageX - shiftX);
            $scope.$apply(function() {
              $scope.dragging = true;
            });
          }

          const updatePosition = function(e) {
            if (isDragging) {
              positionElement(e.pageY - shiftY, e.pageX - shiftX);
              requestAnimationFrame(updatePosition);
            }
          }

          const onMouseUp = function(e) {
            $document.off('pointermove', onMouseMove);
            $element.css('cursor', 'grab');
            $element[0].releasePointerCapture(e.pointerId); 
            setTimeout(function() {
              isDragging = false;
              $scope.$apply(function() {
                $scope.dragging = false;
              });
            }, 50); //Delay to prevent click from firing right after drag

            localStorage.setItem('floatingButtonPosition', JSON.stringify({
              top: $element[0].offsetTop,
              left: $element[0].offsetLeft
            }));
          }

          $element.on('pointerdown', onMouseDown);
          $document.on('pointerup', onMouseUp);
          angular.element($window).on('resize', (e) => {
            positionToStoredState()
          });

        

          $element.on('$destroy', function() {
              $document.off('pointermove', onMouseMove);
              $document.off('pointerup', onMouseUp);
              angular.element($window).off('resize', positionToStoredState);
              $element.off('pointerdown', onMouseDown);
          });
      }
  };
}]);
