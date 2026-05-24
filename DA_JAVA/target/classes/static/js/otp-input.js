(function () {
    "use strict";

    document.querySelectorAll("[data-otp-form]").forEach(function (form) {
        var inputs = Array.prototype.slice.call(form.querySelectorAll("[data-otp-digit]"));
        var valueInput = form.querySelector("[data-otp-value]");

        function setCode(code) {
            var digits = String(code || "").replace(/\D/g, "").slice(0, inputs.length).split("");
            inputs.forEach(function (input, index) {
                input.value = digits[index] || "";
            });
            valueInput.value = digits.join("");
            if (digits.length > 0) {
                inputs[Math.min(digits.length, inputs.length) - 1].focus();
            }
        }

        function syncCode() {
            valueInput.value = inputs.map(function (input) {
                return input.value;
            }).join("");
        }

        inputs.forEach(function (input, index) {
            input.addEventListener("input", function () {
                input.value = input.value.replace(/\D/g, "").slice(-1);
                syncCode();
                if (input.value && index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            });
            input.addEventListener("keydown", function (event) {
                if (event.key === "Backspace" && !input.value && index > 0) {
                    inputs[index - 1].focus();
                }
                if (event.key === "ArrowLeft" && index > 0) {
                    inputs[index - 1].focus();
                }
                if (event.key === "ArrowRight" && index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            });
            input.addEventListener("paste", function (event) {
                event.preventDefault();
                setCode(event.clipboardData.getData("text"));
            });
        });

        form.addEventListener("submit", function (event) {
            syncCode();
            if (!/^\d{6}$/.test(valueInput.value)) {
                event.preventDefault();
                inputs.find(function (input) { return !input.value; }).focus();
            }
        });
    });
})();
