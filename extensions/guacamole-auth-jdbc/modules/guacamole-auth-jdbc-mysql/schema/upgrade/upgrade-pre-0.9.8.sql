--
-- Copyright (C) 2015 Glyptodon LLC
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.
--

--
-- Add per-user time-based access restrictions.
--

ALTER TABLE guacamole_user ADD COLUMN access_window_start    TIME;
ALTER TABLE guacamole_user ADD COLUMN access_window_end      TIME;

--
-- Add per-user date-based account validity restrictions.
--

ALTER TABLE guacamole_user ADD COLUMN valid_from  DATE;
ALTER TABLE guacamole_user ADD COLUMN valid_until DATE;

--
-- Add per-user timezone for sake of time comparisons/interpretation.
--

ALTER TABLE guacamole_user ADD COLUMN timezone VARCHAR(64);
